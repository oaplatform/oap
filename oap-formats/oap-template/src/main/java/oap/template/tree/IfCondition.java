package oap.template.tree;

import javax.annotation.Nullable;

public class IfCondition {
    public final ConditionExpr condition;
    public final Exprs thenCode;
    public final Exprs elseCode;

    public IfCondition( ConditionExpr condition, Exprs thenCode, @Nullable Exprs elseCode ) {
        this.condition = condition;
        this.thenCode = thenCode;
        this.elseCode = elseCode;
    }
}
