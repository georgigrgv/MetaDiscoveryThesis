package org.processmining.contexts.uitopia;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.impl.AbstractGlobalContext;

public class UIPluginContextFactory extends AbstractGlobalContext {
    protected PluginContext getMainPluginContext() {
// TODO Auto-generated method stub
        return null;
    }

    public Class<? extends PluginContext> getPluginContextType() {
// TODO Auto-generated method stub
        return null;
    }

    public UIPluginContext getContext() {
        return new UIPluginContext(this, "");
    }
}