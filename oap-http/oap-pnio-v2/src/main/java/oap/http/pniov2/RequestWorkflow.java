/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.http.pniov2;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class RequestWorkflow<WorkflowState> {
    Node<WorkflowState> root;

    private RequestWorkflow( Node<WorkflowState> root ) {
        this.root = root;
    }

    public static <WorkflowState> RequestWorkflowBuilder<WorkflowState> init( PnioRequestHandler<WorkflowState> task ) {
        RequestWorkflow<WorkflowState> workflow = new RequestWorkflow<>( new Node<>( task, null ) );
        return new RequestWorkflowBuilder<>( workflow );
    }

    public RequestWorkflow<WorkflowState> skipBefore( Predicate<PnioRequestHandler<WorkflowState>> predicate ) {
        Node<WorkflowState> current = root;

        while( current != null ) {
            if( predicate.test( current.handler ) ) {
                return new RequestWorkflow<>( current );
            }

            current = current.next;
        }

        return new RequestWorkflow<>( null );
    }

    public <T> List<T> map( Function<PnioRequestHandler<WorkflowState>, T> mapFunc ) {
        ArrayList<T> ret = new ArrayList<T>();
        Node<WorkflowState> current = root;
        while( current != null ) {
            ret.add( mapFunc.apply( current.handler ) );
            current = current.next;
        }
        return ret;
    }

    public void forEach( Consumer<PnioRequestHandler<WorkflowState>> cons ) {
        Node<WorkflowState> current = root;
        while( current != null ) {
            cons.accept( current.handler );
            current = current.next;
        }
    }

    public void update( RequestWorkflow<WorkflowState> newWorkflow ) {
        root = newWorkflow.root;
    }

    @AllArgsConstructor
    static class Node<WorkflowState> {
        final PnioRequestHandler<WorkflowState> handler;
        Node<WorkflowState> next;
    }

    public static class RequestWorkflowBuilder<WorkflowState> {
        private final RequestWorkflow<WorkflowState> workflow;
        private Node<WorkflowState> lastNode;

        public RequestWorkflowBuilder( RequestWorkflow<WorkflowState> workflow ) {
            this.workflow = workflow;
            lastNode = workflow.root;
        }

        public RequestWorkflowBuilder<WorkflowState> next( PnioRequestHandler<WorkflowState> handlers ) {
            var node = new Node<>( handlers, null );
            lastNode.next = node;
            lastNode = node;

            return this;
        }

        public <T> RequestWorkflowBuilder<WorkflowState> next( List<T> list, Function<T, PnioRequestHandler<WorkflowState>> next ) {
            return next( list, next, null );
        }

        public <T> RequestWorkflowBuilder<WorkflowState> next( List<T> list, Function<T, PnioRequestHandler<WorkflowState>> next, PnioRequestHandlerPostProcess<WorkflowState> postProcess ) {
            for( T item : list ) {
                next( next.apply( item ) );
            }

            if( postProcess != null ) {
                next( new PnioRequestHandler<>( PnioRequestHandler.Type.COMPUTE ) {
                    @Override
                    public void handle( PnioExchange<WorkflowState> pnioExchange, WorkflowState workflowState ) {
                        postProcess.accept( pnioExchange, workflowState );
                    }

                    @Override
                    public String description() {
                        return postProcess.description();
                    }
                } );
            }

            return this;
        }

        public RequestWorkflow<WorkflowState> build() {
            return workflow;
        }

        public interface PnioRequestHandlerPostProcess<WorkflowState> extends BiConsumer<PnioExchange<WorkflowState>, WorkflowState> {
            String description();
        }
    }
}
