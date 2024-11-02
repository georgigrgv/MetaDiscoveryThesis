package algorithm.factory;

import org.processmining.contexts.cli.CLIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.impl.AbstractGlobalContext;
//Dummy PluginContext required by ProM
public class PluginContextFactory extends AbstractGlobalContext {
    protected PluginContext getMainPluginContext() {

        return null;
    }

    public Class<? extends PluginContext> getPluginContextType() {
        return null;
    }

    public CLIPluginContext getContext() {
        return new CLIPluginContext(this, "ciao");
    }
}
