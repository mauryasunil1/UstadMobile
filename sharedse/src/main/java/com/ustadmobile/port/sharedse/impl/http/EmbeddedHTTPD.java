package com.ustadmobile.port.sharedse.impl.http;


import com.ustadmobile.core.db.UmAppDatabase;
import com.ustadmobile.core.impl.UMLog;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.core.util.UMFileUtil;
import com.ustadmobile.lib.db.entities.Container;
import com.ustadmobile.port.sharedse.container.ContainerManager;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;

/**
 * Embedded HTTP Server which runs to serve files directly out of a zipped container on the fly
 *
 * Mounted zips will be acessible under http://IP:PORT/mount/mountName
 *
 * For performance reasons mounted zip files are served with cache headers with a max-age to prevent
 * additional requests - therefor the mountName should include a date or timestamp component to prevent
 * stale files being served.
 *
 * Created by mike on 8/14/15.
 */
public class EmbeddedHTTPD extends RouterNanoHTTPD {

    private int id;

    public static int idCounter = 0;

    public static final String PREFIX_MOUNT = "/mount/";

    private UmAppDatabase appDatabase;

    private UmAppDatabase repository;


    public interface ResponseListener {

        void responseStarted(NanoHTTPD.IHTTPSession session, NanoHTTPD.Response response);

        void responseFinished(NanoHTTPD.IHTTPSession session, NanoHTTPD.Response response);

    }

    private final Vector<ResponseListener> responseListeners = new Vector<>();

    /**
     * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
     */
    @SuppressWarnings("rawtypes")
    private static HashMap<String, String> theMimeTypes = new HashMap<>();

    private Hashtable<String, ZipFile> mountedZips = new Hashtable<>();

    private Hashtable<String, ContainerManager> mountedContainers = new Hashtable<>();

    static {
        theMimeTypes.put("htm", "text/html");
        theMimeTypes.put("html", "text/html");
        theMimeTypes.put("xhtml", "application/xhtml+xml");
        theMimeTypes.put("xml", "text/xml");
        theMimeTypes.put("txt", "text/plain");
        theMimeTypes.put("webp", "image/webp");
        theMimeTypes.put("webm", "video/webm");

        StringTokenizer st = new StringTokenizer(
                "css		text/css " +
                        "asc		text/plain " +
                        "gif		image/gif " +
                        "jpg		image/jpeg " +
                        "jpeg		image/jpeg " +
                        "png		image/png " +
                        "mp3		audio/mpeg " +
                        "m3u		audio/mpeg-url " +
                        "mp4		video/mp4 " +
                        "m4v        video/mp4 " +
                        "ogv		video/ogg " +
                        "flv		video/x-flv " +
                        "mov		video/quicktime " +
                        "swf		application/x-shockwave-flash " +
                        "js			application/javascript " +
                        "pdf		application/pdf " +
                        "doc		application/msword " +
                        "ogg		application/x-ogg " +
                        "zip		application/octet-stream " +
                        "exe		application/octet-stream " +
                        "wav		audio/wav " +
                        "class		application/octet-stream " +
                        "docx       application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        while (st.hasMoreTokens())
            theMimeTypes.put(st.nextToken(), st.nextToken());
    }


    public EmbeddedHTTPD(int portNum, Object context, UmAppDatabase appDatabase, UmAppDatabase repository) {
        super(portNum);
        id = idCounter;
        idCounter++;
        addRoute("/ContainerEntryFile/(.*)+", ContainerEntryFileResponder.class, appDatabase);
        addRoute("/ContainerEntryList/findByContainerWithMd5(.*)+",
                ContainerEntryListResponder.class, appDatabase);
        addRoute("/xAPI/statements(.*)+", XapiStatementResponder.class, repository);
        addRoute("/xAPI/activities/state(.*)+", XapiStateResponder.class, repository);
        this.appDatabase = appDatabase;
        this.repository = repository;
    }

    public EmbeddedHTTPD(int portNum, Object context, UmAppDatabase appDatabase) {
        this(portNum, context, appDatabase, appDatabase.getRepository("http://localhost/dummy/", ""));
    }

    public EmbeddedHTTPD(int portNum, Object context) {
        this(portNum, context, UmAppDatabase.getInstance(context));
    }


    @Override
    public Response serve(IHTTPSession session) {
        final NanoHTTPD.Response response = super.serve(session);
        if(!responseListeners.isEmpty() && response != null) {
            fireResponseStarted(session, response);
            response.setData(new InputStreamWithCloseListener(response.getData(),
                    () -> fireResponseFinished(session, response)));
        }

        return response;
    }

    @Override
    public void addMappings() {
        super.addMappings();
    }


    public String toString() {
        return "EmbeddedHTTPServer on port : " + getListeningPort() + " id: " + id;
    }

    public static String getMimeType(String uri) {
        String mimeResult = theMimeTypes.get(UMFileUtil.getExtension(uri));
        return mimeResult != null ? mimeResult : "application/octet-stream";
    }


    /**
     * Mount a zip to the given path.  The contents of the zip file will then be accessible by
     * HTTP using http://IP:PORT/mount/mountPath
     *
     * Zips should be unmounted when they are no longer needed.  Depending on how Android feels
     * this service may live on after an activity is finished.  The mounted zip keeps a cached
     * copy of the ZipFile object containing entry names, file sizes, data positions etc.
     *
     * For performance the mountPath should include a time/date component.  All files served will be
     * with cache a 1 year maxage cache header
     *
     * @param mountPath The path to use after /mount .
     * @param zipPath The local filesystem path to the zip file (e.g. /path/to/file.epub)
     */
    @Deprecated
    public String mountZip(String zipPath, String mountPath) {
        if (mountPath == null) {
            mountPath = UMFileUtil.getFilename(zipPath) + '-' +
                    new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        }

        try {
            ZipFile zipFile = new ZipFile(zipPath);
            addRoute(PREFIX_MOUNT + mountPath + "/" + MountedZipHandler.URI_ROUTE_POSTFIX,
                    MountedZipHandler.class, zipFile);
            String fullPath = toFullZipMountPath(mountPath);
            mountedZips.put(fullPath, zipFile);
            return toFullZipMountPath(mountPath);
        } catch (ZipException e) {
            UstadMobileSystemImpl.l(UMLog.ERROR, 90, zipPath, e);
        }

        return null;
    }

    public String mountContainer(long containerUid, String mountPath,
                                 List<MountedContainerResponder.MountedContainerFilter> filters) {
        Container container = repository.getContainerDao().findByUid(containerUid);
        if (container == null) {
            return null;
        }

        ContainerManager containerManager = new ContainerManager(container, appDatabase, repository);
        if (mountPath == null) {
            mountPath = "/container/" + container.getContainerUid() + "/" +
                    System.currentTimeMillis() + "/";
        }

        addRoute(mountPath + MountedContainerResponder.URI_ROUTE_POSTFIX,
                MountedContainerResponder.class, containerManager, filters);

        return mountPath;
    }

    public String mountContainer(long containerUid, String mountPath) {
        return mountContainer(containerUid, mountPath, new ArrayList<>());
    }

    public void unmountContainer(String mountPath) {
        removeRoute(mountPath + MountedContainerResponder.URI_ROUTE_POSTFIX);
    }

    private String toFullZipMountPath(String mountPath) {
        try {
            return PREFIX_MOUNT + URLEncoder.encode(mountPath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            //Should enver happen
            UstadMobileSystemImpl.l(UMLog.ERROR, 0, null, e);
        }

        return null;
    }

    /**
     * Unmount a zip that was mounted with mountZip
     *
     * @param mountPath The mount path given to mount the zip
     */
    public void unmountZip(String mountPath) {
        String encodedPath = mountPath.substring(PREFIX_MOUNT.length());
        try {
            String route = PREFIX_MOUNT + URLDecoder.decode(encodedPath, "UTF-8") + "/"
                    + MountedZipHandler.URI_ROUTE_POSTFIX;
            removeRoute(route);
            mountedZips.remove(toFullZipMountPath(mountPath));
        } catch (UnsupportedEncodingException e) {
            UstadMobileSystemImpl.l(UMLog.ERROR, 20, mountPath, e);
        }
    }

    /**
     * Convenience method to make the ZipFile object accessible if a presenter needs it after
     * mounting it on http. This will avoid having to read the file again.
     *
     * @param mountPath The path as returned by mountZip
     * @return ZipFile object for the zip that was mounted on that path, null if it's not mounted.
     */
    public ZipFile getMountedZip(String mountPath) {
        return mountedZips.get(mountPath);
    }

    /**
     * Returns the local URL in the form of http://localhost;PORT/
     *
     * @return Local URL as above including a trailing slash
     */
    public String getLocalURL() {
        return "http://localhost:" + getListeningPort() + "/";
    }

    /**
     * Add an entry response listener. This will receive response events when entries are sent to
     * clients.
     *
     * @param listener
     */
    public void addResponseListener(ResponseListener listener) {
        responseListeners.add(listener);
    }

    /**
     * Remove an entry response listener.
     *
     * @param listener
     */
    public void removeResponseListener(ResponseListener listener) {
        responseListeners.remove(listener);
    }

    protected void fireResponseStarted(NanoHTTPD.IHTTPSession session, NanoHTTPD.Response response) {
        synchronized (responseListeners) {
            for(ResponseListener listener : responseListeners) {
                listener.responseStarted(session, response);
            }
        }
    }

    protected void fireResponseFinished(NanoHTTPD.IHTTPSession session, NanoHTTPD.Response response) {
        synchronized (responseListeners) {
            for(ResponseListener listener: responseListeners) {
                listener.responseFinished(session, response);
            }
        }
    }

    /**
     * Get the local HTTP server url with the URL as it is to be used for access over the loopback
     * interface
     *
     * @return Local http server url e.g. http://127.0.0.1:PORT/
     */
    public String getLocalHttpUrl() {
        return "http://127.0.0.1:" + getListeningPort() + "/";
    }

    /**
     * Mount a Zip File to the http server.  Optionally specify a preferred mount point (useful if
     * the activity is being created from a saved state)
     * <p>
     * ***PORTED FROM NetworkManager***. TODO: refactor / clean this up somewhat.
     *
     * @param zipPath   Path to the zip that should be mounted (mandatory)
     * @param mountName Directory name that this should be mounted as e.g. something.epub-timestamp. Can be null
     * @return The mountname that was used - the content will then be accessible on getZipMountURL()/return value
     */
    public String mountZipOnHttp(String zipPath, String mountName) {
        UstadMobileSystemImpl.l(UMLog.INFO, 371, "Mount zip " + zipPath + " on service "
                + this + "httpd server = " + this + " listening port = " + getListeningPort());

        mountName = mountZip(zipPath, mountName);
        return mountName;
    }
}
