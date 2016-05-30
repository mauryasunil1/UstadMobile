/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ustadmobile.port.j2me.view.exequizsupport;

import com.sun.lwuit.html.HTMLComponent;
import com.sun.lwuit.html.HTMLElement;
import com.ustadmobile.core.impl.UMLog;
import com.ustadmobile.core.impl.UstadMobileSystemImpl;
import com.ustadmobile.port.j2me.view.ContainerViewHTMLCallback;
import com.ustadmobile.port.j2me.view.ContainerViewJ2ME;
import java.util.Vector;
import org.json.me.JSONException;
import org.json.me.JSONObject;

/**
 * Represents a Quiz Idevice element created using eXeLearning
 * 
 * @author mike
 */
public class EXEQuizIdevice {
    Vector questions;
    
    HTMLComponent htmlC;
    
    private String id;
    
    protected int ideviceIndex;
    
    Object context;
    
    String pageTinCanID;
        
    private String registrationUUID;
    
    private JSONObject state;
    
    /**
     * Represents an idevice made in eXeLearning that can have multiple questions
     * 
     * @param ideviceEl The HTMLElement that is the container of the idevice itself
     * (e.g. section/div element with id="idXX" where XX is the idevice id number
     * generated by eXeLearning)
     * @param htmlC The HTML Component showing
     * @param context the current context used for accessing systemimpl
     * @param pageTinCanID The TinCan ID of the page that contains this idevice
     * @param ideviceIndex 
     */
    public EXEQuizIdevice(HTMLElement ideviceEl, HTMLComponent htmlC, Object context, String pageTinCanID,int ideviceIndex) {
        this.htmlC = htmlC;
        this.ideviceIndex = ideviceIndex;
        this.context = context;
        this.pageTinCanID = pageTinCanID;
        setupFromElement(ideviceEl);
    }
    
    /**
     * Gets the id of this idevice e.g. 20 as generated by eXeLearning
     * 
     * @return ID of the idevice as generated by eXeLearning
     */
    public String getID() {
        return id;
    }
    
    /**
     * Set the registration UUID to use in Experience API statements when
     * a question is answered
     * 
     * @param registrationUUID Registration UUID to use
     */
    public void setRegistrationUUID(String registrationUUID) {
        this.registrationUUID = registrationUUID;
    }
    
    /**
     * Gets the registration UUID to use for Experience API statements
     * when a question is answered
     * 
     * @return UUID as above
     */
    public String getRegistrationUUID() {
        return registrationUUID;
    }

    public JSONObject getState() {
        return state;
    }

    /**
     * Takes in the state saved by the Container so that we can restore the 
     * user's previously given responses.
     * 
     * @param state 
     */
    public void setState(JSONObject state) {
        this.state = state;
        EXEQuizQuestion question;
        String questionId;
        for(int i = 0; i < questions.size(); i++) {
            question = (EXEQuizQuestion)questions.elementAt(i);
            questionId = question.getID();
            if(state.has("id"+questionId)) {
                try {
                    question.setState(state.getJSONObject("id"+questionId));
                }catch(JSONException e) {
                    UstadMobileSystemImpl.l(UMLog.ERROR, 188, null, e);
                }
            }
        }
        
    }
    
    
    
    
    /**
     * Gets the full TinCan ID of this idevice
     */
    public String getTinCanId() {
        return pageTinCanID + '/' + id;
    }
    
    /**
     * Setup this idevice given the element that is the idevice container itself
     * 
     * @param ideviceEl 
     */
    public void setupFromElement(HTMLElement ideviceEl) {
        //All idevices must have an id attribute in the form of id='idXX' where XX 
        //is the actual idevice ID as eXe has been generating it
        id = ideviceEl.getAttributeById(HTMLElement.ATTR_ID).substring(2);
        questions = new Vector();
        
        //eXeLearning makes id="id20" etc. - chop the id prefix off
        if(id.startsWith("id")) {
            id = id.substring(2);
        }
        
        //First look for the forms that are generated by eXeLearning for each question
        Vector questionForms = ideviceEl.getDescendantsByClass("activity-form", 
                ContainerViewHTMLCallback.MCQ_FORM_TAGIDS);
        Vector forms = ideviceEl.getDescendantsByTagId(ContainerViewHTMLCallback.MCQ_FORM_TAGIDS[0]);
        HTMLElement currentEl;
        String currentName;
        EXEQuizQuestion currentQuestion;
        int qCount = 0;
        for(int i = 0; i < questionForms.size(); i++) {
            currentEl = (HTMLElement)questionForms.elementAt(i);
            currentName = currentEl.getAttributeById(HTMLElement.ATTR_NAME);
            
            if(currentName != null && currentName.startsWith(EXEQuizQuestion.PREFIX_FORMNAME)) {
                // we have found the question form itself
                currentQuestion = new EXEQuizQuestion(currentEl, this, qCount);
                questions.addElement(currentQuestion);
                qCount++;
            }
        }
    }
    
    public boolean handleSelectAnswer(HTMLElement inputElement) {
        //In eXeLearning the questionID comes immediately after the option in name
        //e.g. "option20_67" MCQ ID = 20_67
        String questionID  = 
            inputElement.getAttributeById(HTMLElement.ATTR_NAME).substring(6);
        EXEQuizQuestion question = getQuestionById(questionID);
        if(question != null) {
            return question.handleSelectAnswer(inputElement);
        }else {
            return false;
        }
    }
    
    public EXEQuizQuestion getQuestionById(String id) {
        EXEQuizQuestion currentQ;
        for(int i = 0; i < questions.size(); i++) {
            currentQ = (EXEQuizQuestion)questions.elementAt(i);
            if(currentQ.getID().equals(id)) {
                return currentQ;
            }
        }
        
        return null;
    }
    
    public Vector getQuestions() {
        return questions;
    }
    
    /**
     * Reformat the questions into a table - the original eXeLearning formatting
     * relies on unsupported CSS to show things in the proper position
     * 
     * @return true if the dom was changed (e.g. at least one question)
     */
    public boolean formatQuestionsAsTables() {
        boolean domChanged = false;
        for(int i = 0; i < questions.size(); i++) {
            domChanged = ((EXEQuizQuestion)questions.elementAt(i)).formatQuestionAsTable() || domChanged;
        }
        
        return domChanged;
    }
    
}
