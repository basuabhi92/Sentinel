package org.ab.sentinel;

import org.nanonative.nano.helper.event.EventChannelRegister;

public final class AppEvents {
    private AppEvents() {}

    // App-level events
    public static final int USER_REGISTER = EventChannelRegister.registerChannelId("USER_REGISTER");

    public static final int USER_LOGIN = EventChannelRegister.registerChannelId("USER_LOGIN");

    public static final int APP_LINK = EventChannelRegister.registerChannelId("APP_LINK");
    public static final int NOTIF_INGESTED = EventChannelRegister.registerChannelId("NOTIF_INGESTED");

    // DB events
    public static final int DB_REQUEST = EventChannelRegister.registerChannelId("DB_REQUEST");
    public static final int DB_RESPONSE = EventChannelRegister.registerChannelId("DB_RESPONSE");
}
