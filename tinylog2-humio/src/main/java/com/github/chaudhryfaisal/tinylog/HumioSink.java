package com.github.chaudhryfaisal.tinylog;

import com.github.chaudhryfaisal.Humio;
import com.github.chaudhryfaisal.batch.Sink;
import com.github.chaudhryfaisal.dto.Event;
import com.github.chaudhryfaisal.dto.EventPayload;
import lombok.AllArgsConstructor;

import java.util.Collection;

/**
 * HumioSink to write bulk events to Humio Backend
 */

@AllArgsConstructor
public final class HumioSink implements Sink< EventPayload, Event> {

    private final Humio humio;

    @Override
    public void write(EventPayload payload) {
        humio.write(payload);
    }

    @Override
    public EventPayload listToBulkPayload(Collection<Event> events) {
        return EventPayload.builder().tags(humio.getTags()).events(events).build();
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {
        humio.close();
    }
}
