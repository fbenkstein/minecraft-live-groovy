package com.frank.livegroovy;

import groovy.lang.Script;

public abstract class LiveGroovyScript extends Script {
    private ScriptScope scope;

    public void setScope(ScriptScope scope) {
        this.scope = scope;
    }

    public EventRegistry getEvents() {
        return scope.events();
    }

    public CommandRegistry getCommands() {
        return scope.commands();
    }

    public SchedulerApi getScheduler() {
        return scope.scheduler();
    }

    public PaletteItems getItems() {
        return scope.api().items();
    }

    public LiveGroovyApi getApi() {
        return scope.api();
    }
}
