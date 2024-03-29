package org.apache.maven.model.composition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.codehaus.plexus.component.annotations.Component;
@Component( role = DependencyManagementImporter.class )
public class DefaultDependencyManagementImporter
    implements DependencyManagementImporter
{
    public void importManagement( Model target, List<? extends DependencyManagement> sources,
                                  ModelBuildingRequest request, ModelProblemCollector problems )
    {
        if ( sources != null && !sources.isEmpty() )
        {
            Map<String, Dependency> dependencies = new LinkedHashMap<String, Dependency>();
            DependencyManagement depMngt = target.getDependencyManagement();
            if ( depMngt != null )
            {
                for ( Dependency dependency : depMngt.getDependencies() )
                {
                    dependencies.put( dependency.getManagementKey(), dependency );
                }
            }
            else
            {
                depMngt = new DependencyManagement();
                target.setDependencyManagement( depMngt );
            }
            for ( DependencyManagement source : sources )
            {
                for ( Dependency dependency : source.getDependencies() )
                {
                    String key = dependency.getManagementKey();
                    if ( !dependencies.containsKey( key ) )
                    {
                        dependencies.put( key, dependency );
                    }
                }
            }
            depMngt.setDependencies( new ArrayList<Dependency>( dependencies.values() ) );
        }
    }
}
