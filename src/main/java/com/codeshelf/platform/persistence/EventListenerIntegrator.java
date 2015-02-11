package com.codeshelf.platform.persistence;

import lombok.Getter;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.ObjectChangeBroadcaster;

public class EventListenerIntegrator implements Integrator {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(EventListenerIntegrator.class);

	@Getter
	private ObjectChangeBroadcaster changeBroadcaster;
	
	public EventListenerIntegrator(ObjectChangeBroadcaster changeBroadcaster) {
		super();
		this.changeBroadcaster = changeBroadcaster;
	}

	@Override
	public void integrate(Configuration arg0, SessionFactoryImplementor arg1, SessionFactoryServiceRegistry serviceRegistry) {
		// As you might expect, an EventListenerRegistry is the thing with which event listeners are registered  It is a
        // service so we look it up using the service registry
        final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );

        // If you wish to have custom determination and handling of "duplicate" listeners, you would have to add an
        // implementation of the org.hibernate.event.service.spi.DuplicationStrategy contract like this
        //eventListenerRegistry.addDuplicationStrategy( myDuplicationStrategy );

        // EventListenerRegistry defines 3 ways to register listeners:
        //     1) This form overrides any existing registrations with
        //eventListenerRegistry.setListeners( EventType.AUTO_FLUSH, myCompleteSetOfListeners );
        //     2) This form adds the specified listener(s) to the beginning of the listener chain
        //eventListenerRegistry.prependListeners( EventType.UPDATE, EventListener.class);
        eventListenerRegistry.prependListeners( EventType.POST_COMMIT_UPDATE, new UpdateBroadcastListener(changeBroadcaster));
        eventListenerRegistry.prependListeners( EventType.POST_COMMIT_DELETE, new DeleteBroadcastListener(changeBroadcaster));
        eventListenerRegistry.prependListeners( EventType.POST_COMMIT_INSERT, new InsertBroadcastListener(changeBroadcaster));
        //eventListenerRegistry.prependListeners( EventType.DELETE, EventListener.class);
        //     3) This form adds the specified listener(s) to the end of the listener chain
        //eventListenerRegistry.appendListeners( EventType.AUTO_FLUSH, myListenersToBeCalledLast );
        LOGGER.info("Event listeners registered");
        
	}

	@Override
	public void integrate(MetadataImplementor arg0, SessionFactoryImplementor arg1, SessionFactoryServiceRegistry arg2) {
		throw new UnsupportedOperationException("unexpected call of integrate");
		
	}

	@Override
	public void disintegrate(SessionFactoryImplementor arg0, SessionFactoryServiceRegistry serviceRegistry) {
		//TODO determine if we need to go removing our listeners practically speaking
	}

}