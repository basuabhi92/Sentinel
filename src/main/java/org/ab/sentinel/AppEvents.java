package org.ab.sentinel;

import org.nanonative.nano.helper.event.EventChannelRegister;

public final class AppEvents {
    private AppEvents() {}

    // App events
    public static final int USER_REGISTER = EventChannelRegister.registerChannelId("USER_REGISTER");
    public static final int USER_LOGIN = EventChannelRegister.registerChannelId("USER_LOGIN");
    public static final int APP_LIST = EventChannelRegister.registerChannelId("APP_LIST");
    public static final int APP_INT_REQ = EventChannelRegister.registerChannelId("APP_INT_REQ");
    public static final int GITHUB_INT_REQ = EventChannelRegister.registerChannelId("GITHUB_INT_REQ");
    public static final int NOTIF_INGESTED = EventChannelRegister.registerChannelId("NOTIF_INGESTED");
}
