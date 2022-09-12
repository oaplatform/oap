/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.http.pnio;

import lombok.AllArgsConstructor;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class RequestWorkflow<WorkflowState> {
    @AllArgsConstructor
    static class Node<WorkflowState> {
        final AbstractRequestTask<WorkflowState> task;
        Node<WorkflowState> next;
    }

    Node<WorkflowState> root;

    private RequestWorkflow( Node<WorkflowState> root ) {
        this.root = root;
    }

    public static <WorkflowState> RequestWorkflowBuilder<WorkflowState> init( AbstractRequestTask<WorkflowState> task ) {
        RequestWorkflow<WorkflowState> workflow = new RequestWorkflow<>( new Node<>( task, null ) );
        return new RequestWorkflowBuilder<>( workflow );
    }

    public static class RequestWorkflowBuilder<WorkflowState> {
        private final RequestWorkflow<WorkflowState> workflow;
        private Node<WorkflowState> lastNode;

        public RequestWorkflowBuilder( RequestWorkflow<WorkflowState> workflow ) {
            this.workflow = workflow;
            lastNode = workflow.root;
        }

        public RequestWorkflowBuilder<WorkflowState> next( AbstractRequestTask<WorkflowState> task ) {
            var node = new Node<>( task, null );
            lastNode.next = node;
            lastNode = node;

            return this;
        }

        public <T> RequestWorkflowBuilder<WorkflowState> next( List<T> list, Function<T, AbstractRequestTask<WorkflowState>> next ) {
            return next( list, next, null );
        }

        public <T> RequestWorkflowBuilder<WorkflowState> next( List<T> list, Function<T, AbstractRequestTask<WorkflowState>> next, BiConsumer<RequestTaskState<WorkflowState>, WorkflowState> postProcess ) {
            for( var item : list ) {
                next( next.apply( item ) );
            }

            if( postProcess != null ) {
                next( new AbstractRequestTask<WorkflowState>() {
                    @Override
                    public boolean isCpu() {
                        return true;
                    }

                    @Override
                    public void accept( RequestTaskState<WorkflowState> requestTaskState, WorkflowState workflowState ) {
                        postProcess.accept( requestTaskState, workflowState );
                    }
                } );
            }

            return this;
        }

        public RequestWorkflow<WorkflowState> build( AbstractResponseTask<WorkflowState> task ) {
            next( task );
            return workflow;
        }
    }

    public void update( RequestWorkflow<WorkflowState> newWorkflow ) {
        root = newWorkflow.root;
    }
}