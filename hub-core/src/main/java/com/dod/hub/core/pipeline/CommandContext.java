package com.dod.hub.core.pipeline;

import com.dod.hub.core.command.HubCommand;
import com.dod.hub.core.provider.HubProvider;
import com.dod.hub.core.provider.ProviderSession;

public class CommandContext {
    private final ProviderSession session;
    private final HubProvider provider;
    private final HubCommand command;

    public CommandContext(ProviderSession session, HubProvider provider, HubCommand command) {
        this.session = session;
        this.provider = provider;
        this.command = command;
    }

    public ProviderSession getSession() {
        return session;
    }

    public HubProvider getProvider() {
        return provider;
    }

    public HubCommand getCommand() {
        return command;
    }
}
