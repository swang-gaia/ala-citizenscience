package au.com.gaiaresources.bdrs.event;

import java.util.List;

import au.com.gaiaresources.bdrs.util.TransactionHelper;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.hibernate.event.PostCollectionRecreateEvent;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostCollectionUpdateEvent;
import org.hibernate.event.PostCollectionUpdateEventListener;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.service.threshold.ThresholdService;
import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.model.threshold.Action;
import au.com.gaiaresources.bdrs.model.threshold.ActionEvent;
import au.com.gaiaresources.bdrs.model.threshold.Condition;
import au.com.gaiaresources.bdrs.model.threshold.Threshold;
import au.com.gaiaresources.bdrs.model.threshold.ThresholdDAO;

/**
 * The <code>ThresholdEventListener</code> is triggered whenever a insert, 
 * update or delete occurs on an object. If thresholds have been created for
 * that class of object then the conditions on each threshold are applied to the
 * object. If the conditions for the threshold pass, the registered actions
 * for the threshold are applied. 
 */
public class ThresholdEventListener implements PostUpdateEventListener,
        PostInsertEventListener, PostCollectionUpdateEventListener,
        PostCollectionRecreateEventListener {

    private static final long serialVersionUID = 5389414102803552714L;

    @SuppressWarnings("unused")
    private transient Logger log = Logger.getLogger(getClass());

    @Autowired
    private transient ThresholdDAO thresholdDAO;

    @Autowired
    private transient ThresholdService thresholdService;

    @Autowired
    private SessionFactory sessionFactory;

    public ThresholdEventListener() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        triggerUpdateOrInsert(event.getEntity(), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPostInsert(PostInsertEvent event) {
        triggerUpdateOrInsert(event.getEntity(), true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
        triggerUpdateOrInsert(event.getAffectedOwnerEntityName(), false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
        triggerUpdateOrInsert(event.getAffectedOwnerEntityName(), false);
    }

    private void triggerUpdateOrInsert(Object entity, boolean isInsert) {
        if ((entity != null) && (entity instanceof PersistentImpl) && (((PersistentImpl)entity).getId() != null)) {
            PersistentImpl original = (PersistentImpl)entity;
            
            // Don't run the threshold tests...
            if (!original.isRunThreshold()) {
                return;
            }
            // set the run threshold to false so thresholds don't run again for this object
            // this is to avoid triggering the thresholds twice on insert because of the 
            // hibernate insert and subsequent update 
            // this has the unfortunate side effect of preventing thresholds from running
            // twice within one session on insert/update
            // if you want to do that, set this to true in your caller after your first save and before your second
            original.setRunThreshold(false);
            
            if (thresholdService.isRegisteredReference(original)) {
                // Triggered by an action.
            } else {
                
                Session sesh = sessionFactory.openSession();
                Transaction tx = sesh.beginTransaction();
                
                PersistentImpl persistent = (PersistentImpl) sesh.get(entity.getClass(), ((PersistentImpl) entity).getId());
                
                // Test if the transaction was rolled back
                if(persistent != null && entity.equals(persistent)) {
                    thresholdService.registerReference(persistent);
                    
                    // Executing Thresholds
                    String className = persistent.getClass().getCanonicalName();
                    List<Threshold> thresholdList = thresholdDAO.getEnabledThresholdByClassName(sesh, className);
                    for (Threshold threshold : thresholdList) {
                        
                        boolean conditionsPassed = true;
                        for (Condition condition : threshold.getConditions()) {
                            conditionsPassed = conditionsPassed
                                    && condition.applyCondition(sesh, persistent, thresholdService);
                        }
                        
                        // Conditions Passed. Applying Actions
                        if (conditionsPassed) {

                            for (Action action : threshold.getActions()) {
                                // only run the action if it is create and update or
                                // it is create and the created and updated time are the same or
                                // it is update and the created and updated time are different
                                if (action.getActionEvent().equals(ActionEvent.CREATE_AND_UPDATE) ||
                                        (action.getActionEvent().equals(ActionEvent.CREATE) && isInsert) ||
                                        (action.getActionEvent().equals(ActionEvent.UPDATE) && !isInsert))
                                    thresholdService.applyAction(sesh, threshold, persistent, action);
                            }
                        }
                    }
                } 
                
                // It is imperative that the transaction is committed before the
                // persistent is deregistered. Any events that get triggered
                // via action will be fired on commit. Only after that point
                // can the object be deregistered.
                TransactionHelper.commit(tx, sesh);
                sesh.close();
                
                // Removing an object that possibly was not registered
                thresholdService.deregisterReference(persistent);
            }
        }
    }
}
