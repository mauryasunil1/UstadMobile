package com.ustadmobile.core.db

import com.ustadmobile.door.DoorDatabaseCallback
import com.ustadmobile.door.DoorDbType
import com.ustadmobile.door.DoorSqlDatabase
import com.ustadmobile.door.ext.dbType
import com.ustadmobile.door.ext.execSqlBatch

/**
 * These triggers manage recursive progress tracking for ContentJobItem. Each ContentJobItem
 * can have multiple children, e.g.
 *
 * ContentJobItem (parent)
 *  - ContentJobItem (child)
 *  - ContentJobItem (child)
 *    - ContentJobItem (grandchild)
 *
 * A ContentJobItem can contain thousands of child items. Normally if the user wants to see the
 * progress of the parent item, this would create a complex recursive query (infeasible to use
 * to display regular progress).
 *
 * The recursive trigger system works by posting progress up the tree when an update occurs.
 */
class ContentJobItemTriggersCallback: DoorDatabaseCallback {

    override fun onCreate(db: DoorSqlDatabase) {
        if(db.dbType() == DoorDbType.SQLITE) {
            db.execSqlBatch(arrayOf(
                """
                CREATE TRIGGER ContentJobItem_InsertTrigger 
                AFTER INSERT ON ContentJobItem
                BEGIN
                UPDATE ContentJobItem 
                   SET cjiRecursiveProgress = NEW.cjiItemProgress,
                       cjiRecursiveTotal = NEW.cjiItemTotal
                WHERE ContentJobItem.cjiUid = NEW.cjiUid;
                END;
                """,
                """
                CREATE TRIGGER ContentJobItem_UpdateRecursiveTotals 
                AFTER UPDATE ON ContentJobItem
                FOR EACH ROW WHEN (
                    NEW.cjiItemProgress != OLD.cjiItemProgress
                        OR NEW.cjiItemTotal != OLD.cjiItemTotal)
                BEGIN
                UPDATE ContentJobItem 
                   SET cjiRecursiveProgress = (cjiRecursiveProgress + (NEW.cjiItemProgress - OLD.cjiItemProgress)),
                       cjiRecursiveTotal = (cjiRecursiveTotal + (NEW.cjiItemTotal - OLD.cjiItemTotal))
                 WHERE ContentJobItem.cjiUid = NEW.cjiUid;
                END;
                """,
              /*  """
                CREATE TRIGGER ContentJobItem_UpdateRecursiveStatus
                AFTER UPDATE ON ContentJobItem
                FOR EACH ROW WHEN (NEW.cjiStatus != OLD.cjiStatus)
                BEGIN 
                UPDATE ContentJobItem
                    SET cjiRecursiveStatus = $STATUS_CHECK
                    WHERE contentJobItem.cjiUid = NEW.cjiUid;
                END;    
                """,    */
                """
                CREATE TRIGGER ContentJobItem_UpdateParents
                AFTER UPDATE ON ContentJobItem
                FOR EACH ROW WHEN (
                        NEW.cjiParentCjiUid != 0 
                    AND (NEW.cjiRecursiveProgress != OLD.cjiRecursiveProgress
                         OR NEW.cjiRecursiveTotal != OLD.cjiRecursiveTotal))
                BEGIN
                UPDATE ContentJobItem 
                   SET cjiRecursiveProgress = (cjiRecursiveProgress + (NEW.cjiRecursiveProgress - OLD.cjiRecursiveProgress)),
                       cjiRecursiveTotal = (cjiRecursiveTotal + (NEW.cjiRecursiveTotal - OLD.cjiRecursiveTotal))
                 WHERE ContentJobItem.cjiUid = NEW.cjiParentCjiUid;
                END;
                """/*,
                """
                CREATE TRIGGER ContentJobItem_UpdateStatusParent
                AFTER UPDATE ON ContentJobItem
                FOR EACH ROW WHEN (
                         NEW.cjiParentCjiUid != 0
                    AND (New.cjiRecursiveStatus != OLD.cjiRecursiveStatus))
                BEGIN
                UPDATE ContentJobItem
                   SET cjiRecursiveStatus = $STATUS_CHECK
                 WHERE ContentJobItem.cjiUid = NEW.cjiParentCjiUid;
                 END;
                """*/

            ))
        }else {
            db.execSqlBatch(arrayOf(
                """
                CREATE OR REPLACE FUNCTION contentjobiteminsert_fn() RETURNS TRIGGER AS ${'$'}${'$'} 
                BEGIN
                UPDATE ContentJobItem 
                   SET cjiRecursiveProgress = NEW.cjiItemProgress,
                       cjiRecursiveTotal = NEW.cjiItemTotal
                 WHERE ContentJobItem.cjiUid = NEW.cjiUid;
                RETURN NULL; 
                END ${'$'}${'$'} LANGUAGE plpgsql
                """,
                """
                CREATE TRIGGER contentjobiteminsert_trig 
                AFTER INSERT ON ContentJobItem
                FOR EACH ROW EXECUTE PROCEDURE contentjobiteminsert_fn()    
                """,

                """
                CREATE OR REPLACE FUNCTION contentjobitem_updaterecursivetotals_fn() RETURNS TRIGGER AS ${'$'}${'$'}
                BEGIN
                UPDATE ContentJobItem 
                   SET cjiRecursiveProgress = (cjiRecursiveProgress + (NEW.cjiItemProgress - OLD.cjiItemProgress)),
                       cjiRecursiveTotal = (cjiRecursiveTotal + (NEW.cjiItemTotal - OLD.cjiItemTotal))
                 WHERE (NEW.cjiItemProgress != OLD.cjiItemProgress OR NEW.cjiItemTotal != OLD.cjiItemTotal)
                   AND ContentJobItem.cjiUid = NEW.cjiUid;
                RETURN NULL;
                END ${'$'}${'$'} LANGUAGE plpgsql
                """,
                """
                CREATE TRIGGER contentjobitem_updaterecursivetotals_trig
                AFTER UPDATE ON ContentJobItem
                FOR EACH ROW EXECUTE PROCEDURE contentjobitem_updaterecursivetotals_fn();
                """,

                """
                CREATE OR REPLACE FUNCTION contentjobitem_updateparents_fn() RETURNS TRIGGER AS ${'$'}${'$'}
                BEGIN 
                UPDATE ContentJobItem 
                   SET cjiRecursiveProgress = (cjiRecursiveProgress + (NEW.cjiRecursiveProgress - OLD.cjiRecursiveProgress)),
                       cjiRecursiveTotal = (cjiRecursiveTotal + (NEW.cjiRecursiveTotal - OLD.cjiRecursiveTotal))
                 WHERE (NEW.cjiRecursiveProgress != OLD.cjiRecursiveProgress
                        OR NEW.cjiRecursiveTotal != OLD.cjiRecursiveTotal)
                    AND ContentJobItem.cjiUid = NEW.cjiParentCjiUid
                    AND NEW.cjiParentCjiUid != 0;  
                RETURN NULL;
                END ${'$'}${'$'} LANGUAGE plpgsql
                """,
                """
                CREATE TRIGGER contentjobitem_updateparents_trig
                AFTER UPDATE ON ContentJobItem
                FOR EACH ROW EXECUTE PROCEDURE contentjobitem_updateparents_fn();    
                """
            ))
        }

    }

    override fun onOpen(db: DoorSqlDatabase) {

    }

    companion object {

        const val STATUS_CHECK = """
            (CASE cjiStatus 
              WHEN (cjiStatus = ${JobStatus.COMPLETE} 
                        AND (SELECT Count(*) FROM ContentJobItem ContentJobItemInternal
                             WHERE ContentJobItemInternal.cjiStatus = ${JobStatus.COMPLETE}
                               AND ((cjiParentCjiUid = ContentJobItemInternal.cjiUid) 
                                OR cjiUid = ContentJobItemInternal.cjiUid)) == 
                                    (SELECT COUNT(*) FROM ContentJobItem ContentJobItemInternal
                                      WHERE ((cjiParentCjiUid = ContentJobItemInternal.cjiUid) 
                                OR cjiUid = ContentJobItemInternal.cjiUid)))
             THEN ${JobStatus.COMPLETE}
             WHEN (cjiStatus > ${JobStatus.COMPLETE_MIN}
                       AND EXISTS (SELECT * FROM ContentJobItem ContentJobItemInternal
                           WHERE cjiStatus = ${JobStatus.FAILED}
                           AND ((cjiParentCjiUid = ContentJobItemInternal.cjiUid) 
                               OR cjiUid = ContentJobItemInternal.cjiUid))
                       AND EXISTS  (SELECT * FROM ContentJobItem ContentJobItemInternal
                           WHERE cjiStatus = ${JobStatus.COMPLETE}
                           AND ((cjiParentCjiUid = ContentJobItemInternal.cjiUid) 
                               OR cjiUid = ContentJobItemInternal.cjiUid)))             
             THEN ${JobStatus.PARTIAL_FAILED}
             WHEN EXISTS (SELECT * FROM ContentJobItem ContentJobItemInternal
                           WHERE cjiStatus = ${JobStatus.RUNNING} 
                           AND ((cjiParentCjiUid = ContentJobItemInternal.cjiUid) 
                                OR cjiUid = ContentJobItemInternal.cjiUid))
             THEN ${JobStatus.RUNNING}
             WHEN EXISTS (SELECT * FROM ContentJobItem ContentJobItemInternal
                           WHERE cjiStatus = ${JobStatus.WAITING_FOR_CONNECTION}
                           AND ((cjiParentCjiUid = ContentJobItemInternal.cjiUid) 
                               OR cjiUid = ContentJobItemInternal.cjiUid))
             THEN ${JobStatus.WAITING_FOR_CONNECTION}
              WHEN EXISTS (SELECT * FROM ContentJobItem ContentJobItemInternal
                           WHERE cjiStatus = ${JobStatus.FAILED}
                           AND ((cjiParentCjiUid = ContentJobItemInternal.cjiUid) 
                               OR cjiUid = ContentJobItemInternal.cjiUid))
             THEN ${JobStatus.FAILED}
             ELSE ${JobStatus.QUEUED} END)
        """

    }

}