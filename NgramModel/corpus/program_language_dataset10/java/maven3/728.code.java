package org.apache.maven.model.inheritance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.merge.MavenModelMerger;
import org.codehaus.plexus.component.annotations.Component;
@Component( role = InheritanceAssembler.class )
public class DefaultInheritanceAssembler
    implements InheritanceAssembler
{
    private InheritanceModelMerger merger = new InheritanceModelMerger();
    public void assembleModelInheritance( Model child, Model parent, ModelBuildingRequest request,
                                          ModelProblemCollector problems )
    {
        Map<Object, Object> hints = new HashMap<Object, Object>();
        hints.put( MavenModelMerger.CHILD_PATH_ADJUSTMENT, getChildPathAdjustment( child, parent ) );
        merger.merge( child, parent, false, hints );
    }
    private String getChildPathAdjustment( Model child, Model parent )
    {
        String adjustment = "";
        if ( parent != null )
        {
            String childArtifactId = child.getArtifactId();
            for ( String module : parent.getModules() )
            {
                module = module.replace( '\\', '/' );
                if ( module.regionMatches( true, module.length() - 4, ".xml", 0, 4 ) )
                {
                    module = module.substring( 0, module.lastIndexOf( '/' ) + 1 );
                }
                String moduleName = module;
                if ( moduleName.endsWith( "/" ) )
                {
                    moduleName = moduleName.substring( 0, moduleName.length() - 1 );
                }
                int lastSlash = moduleName.lastIndexOf( '/' );
                moduleName = moduleName.substring( lastSlash + 1 );
                if ( moduleName.equals( childArtifactId ) && lastSlash >= 0 )
                {
                    adjustment = module.substring( 0, lastSlash );
                    break;
                }
            }
        }
        return adjustment;
    }
    private static class InheritanceModelMerger
        extends MavenModelMerger
    {
        @Override
        protected void mergePluginContainer_Plugins( PluginContainer target, PluginContainer source,
                                                     boolean sourceDominant, Map<Object, Object> context )
        {
            List<Plugin> src = source.getPlugins();
            if ( !src.isEmpty() )
            {
                List<Plugin> tgt = target.getPlugins();
                Map<Object, Plugin> master = new LinkedHashMap<Object, Plugin>( src.size() * 2 );
                for ( Plugin element : src )
                {
                    if ( element.isInherited() || !element.getExecutions().isEmpty() )
                    {
                        Plugin plugin = new Plugin();
                        plugin.setGroupId( null );
                        mergePlugin( plugin, element, sourceDominant, context );
                        Object key = getPluginKey( element );
                        master.put( key, plugin );
                    }
                }
                Map<Object, List<Plugin>> predecessors = new LinkedHashMap<Object, List<Plugin>>();
                List<Plugin> pending = new ArrayList<Plugin>();
                for ( Plugin element : tgt )
                {
                    Object key = getPluginKey( element );
                    Plugin existing = master.get( key );
                    if ( existing != null )
                    {
                        mergePlugin( element, existing, sourceDominant, context );
                        master.put( key, element );
                        if ( !pending.isEmpty() )
                        {
                            predecessors.put( key, pending );
                            pending = new ArrayList<Plugin>();
                        }
                    }
                    else
                    {
                        pending.add( element );
                    }
                }
                List<Plugin> result = new ArrayList<Plugin>( src.size() + tgt.size() );
                for ( Map.Entry<Object, Plugin> entry : master.entrySet() )
                {
                    List<Plugin> pre = predecessors.get( entry.getKey() );
                    if ( pre != null )
                    {
                        result.addAll( pre );
                    }
                    result.add( entry.getValue() );
                }
                result.addAll( pending );
                target.setPlugins( result );
            }
        }
        @Override
        protected void mergePlugin( Plugin target, Plugin source, boolean sourceDominant, Map<Object, Object> context )
        {
            if ( source.isInherited() )
            {
                mergeConfigurationContainer( target, source, sourceDominant, context );
            }
            mergePlugin_GroupId( target, source, sourceDominant, context );
            mergePlugin_ArtifactId( target, source, sourceDominant, context );
            mergePlugin_Version( target, source, sourceDominant, context );
            mergePlugin_Extensions( target, source, sourceDominant, context );
            mergePlugin_Dependencies( target, source, sourceDominant, context );
            mergePlugin_Executions( target, source, sourceDominant, context );
        }
        @Override
        protected void mergeReporting_Plugins( Reporting target, Reporting source, boolean sourceDominant,
                                               Map<Object, Object> context )
        {
            List<ReportPlugin> src = source.getPlugins();
            if ( !src.isEmpty() )
            {
                List<ReportPlugin> tgt = target.getPlugins();
                Map<Object, ReportPlugin> merged =
                    new LinkedHashMap<Object, ReportPlugin>( ( src.size() + tgt.size() ) * 2 );
                for ( ReportPlugin element :  src )
                {
                    Object key = getReportPluginKey( element );
                    if ( element.isInherited() )
                    {
                        ReportPlugin plugin = new ReportPlugin();
                        plugin.setGroupId( null );
                        mergeReportPlugin( plugin, element, sourceDominant, context );
                        merged.put( key, plugin );
                    }
                }
                for ( ReportPlugin element : tgt )
                {
                    Object key = getReportPluginKey( element );
                    ReportPlugin existing = merged.get( key );
                    if ( existing != null )
                    {
                        mergeReportPlugin( element, existing, sourceDominant, context );
                    }
                    merged.put( key, element );
                }
                target.setPlugins( new ArrayList<ReportPlugin>( merged.values() ) );
            }
        }
    }
}
