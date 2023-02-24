package com.example.wot_servient.wot.thing.event;

import java.util.Optional;

import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

/**
 * This class represented the container for the subject of a {@link ThingEvent}. The subject is used
 * as a resource for new values and observers of the event.
 */
public class EventState<T> {

	private final Subject<Optional<T>> subject;

	public EventState() {
		this(PublishSubject.create());
	}

	EventState(Subject<Optional<T>> subject) {
		this.subject = subject;
	}

	public Subject<Optional<T>> getSubject() {
		return subject;
	}
}
