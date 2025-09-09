package org.ab.sentinel.service;

import berlin.yuna.typemap.model.TypeMapI;
import org.nanonative.nano.core.model.Service;
import org.nanonative.nano.helper.event.model.Event;

public final class RulesService extends Service {

    @Override
    public void stop() {

    }

    @Override
    public Object onFailure(final Event event) {
        return null;
    }

    @Override
    public void onEvent(final Event event) {

    }

    @Override
    public void configure(final TypeMapI<?> typeMapI, final TypeMapI<?> typeMapI1) {

    }

    @Override public String name() { return "RulesService"; }

    @Override
    public void start() {
        context.info(() -> "[" + name() + "] started ");
        // In the next iteration:
        // - subscribe to NOTIF_INGESTED to compute delivery (silent/push/digest)
        // - expose /api/rules CRUD (persist via Events.DB_REQUEST)
    }
}
