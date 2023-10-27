package support.fixture;

import kitchenpos.domain.table.OrderTable;
import kitchenpos.domain.table.OrderTables;
import kitchenpos.domain.table.TableGroup;

import java.util.ArrayList;
import java.util.List;

public class TableGroupBuilder {

    private List<OrderTable> orderTables;

    public TableGroupBuilder() {
        this.orderTables = new ArrayList<>();
    }

    public TableGroupBuilder setOrderTables(final List<OrderTable> orderTables) {
        this.orderTables = orderTables;
        return this;
    }

    public TableGroup build() {
        final OrderTables orderTables = new OrderTables(this.orderTables);
        return new TableGroup(orderTables);
    }
}