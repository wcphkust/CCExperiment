package org.apache.maven.lifecycle.internal;
import junit.framework.TestCase;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub;
import org.apache.maven.project.MavenProject;
import java.util.Iterator;
public class ConcurrentBuildLoggerTest
    extends TestCase
{
    public void testToGraph()
        throws Exception
    {
        ConcurrentBuildLogger concurrentBuildLogger = new ConcurrentBuildLogger();
        MojoDescriptorCreator mojoDescriptorCreator =
            LifecycleExecutionPlanCalculatorTest.createMojoDescriptorCreator();
        LifecycleExecutionPlanCalculator lifecycleExecutionPlanCalculator =
            LifecycleExecutionPlanCalculatorTest.createExecutionPlaceCalculator( mojoDescriptorCreator );
        MavenProject A = ProjectDependencyGraphStub.B;
        MavenProject B = ProjectDependencyGraphStub.C;
        final MavenSession session1 = ProjectDependencyGraphStub.getMavenSession( A );
        final GoalTask goalTask1 = new GoalTask( "compiler:compile" );
        final GoalTask goalTask2 = new GoalTask( "surefire:test" );
        final TaskSegment taskSegment1 = new TaskSegment( false, goalTask1, goalTask2 );
        MavenExecutionPlan executionPlan =
            lifecycleExecutionPlanCalculator.calculateExecutionPlan( session1, A, taskSegment1.getTasks() );
        MavenExecutionPlan executionPlan2 =
            lifecycleExecutionPlanCalculator.calculateExecutionPlan( session1, B, taskSegment1.getTasks() );
        final Iterator<ExecutionPlanItem> planItemIterator = executionPlan.iterator();
        final BuildLogItem a1 = concurrentBuildLogger.createBuildLogItem( A, planItemIterator.next() );
        final BuildLogItem a2 = concurrentBuildLogger.createBuildLogItem( A, planItemIterator.next() );
        final Iterator<ExecutionPlanItem> plan2ItemIterator = executionPlan.iterator();
        final BuildLogItem b1 = concurrentBuildLogger.createBuildLogItem( B, plan2ItemIterator.next() );
        final BuildLogItem b2 = concurrentBuildLogger.createBuildLogItem( B, plan2ItemIterator.next() );
        b1.addDependency( A, "Project dependency" );
        final Iterator<ExecutionPlanItem> aPlan = executionPlan.iterator();
        b1.addWait( A, aPlan.next(), System.currentTimeMillis() );
        b2.addWait( A, aPlan.next(), System.currentTimeMillis() );
        final String response = concurrentBuildLogger.toGraph();
        assertTrue( response.indexOf( "digraph" ) >= 0 );
    }
}
