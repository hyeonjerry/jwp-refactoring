package kitchenpos.application;

import kitchenpos.dao.OrderDao;
import kitchenpos.dao.OrderTableDao;
import kitchenpos.dao.TableGroupDao;
import kitchenpos.domain.OrderStatus;
import kitchenpos.domain.OrderTable;
import kitchenpos.domain.TableGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import support.fixture.OrderBuilder;
import support.fixture.TableBuilder;
import support.fixture.TableGroupBuilder;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TableGroupServiceTest {

    @Autowired
    private TableGroupService tableGroupService;
    @Autowired
    private TableGroupDao tableGroupDao;
    @Autowired
    private OrderTableDao orderTableDao;
    @Autowired
    private OrderDao orderDao;

    @Nested
    @DisplayName("테이블 그룹 생성 테스트")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class CreateTableGroupTest {

        Stream<List<OrderTable>> createWithEmptySmallerThenTwoTablesTest() {
            final OrderTable table = new TableBuilder().build();

            return Stream.of(
                    List.of(),
                    List.of(table)
            );
        }

        @Test
        @DisplayName("테이블 그룹을 생성하고 테이블의 그룹 id를 설정하고 empty를 false로 설장한다.")
        void createTest() {
            // given
            final OrderTable table1 = orderTableDao.save(new TableBuilder().build());
            final OrderTable table2 = orderTableDao.save(new TableBuilder().build());

            final TableGroup tableGroup = new TableGroupBuilder(List.of(table1, table2)).build();

            // when
            final TableGroup expectTableGroup = tableGroupService.create(tableGroup);

            // then
            final TableGroup actualTableGroup = tableGroupDao.findById(expectTableGroup.getId()).get();
            assertThat(actualTableGroup)
                    .usingRecursiveComparison()
                    .ignoringFields("orderTables")
                    .isEqualTo(expectTableGroup);

            orderTableDao.findById(table1.getId()).ifPresentOrElse(
                    actual -> {
                        assertEquals(actualTableGroup.getId(), actual.getTableGroupId());
                        assertFalse(actual.isEmpty());
                    },
                    () -> fail("테이블이 존재하지 않습니다.")
            );

            orderTableDao.findById(table2.getId()).ifPresentOrElse(
                    actual -> {
                        assertEquals(actualTableGroup.getId(), actual.getTableGroupId());
                        assertFalse(actual.isEmpty());
                    },
                    () -> fail("테이블이 존재하지 않습니다.")
            );
        }

        @ParameterizedTest
        @MethodSource
        @DisplayName("OrderTable이 2개 미만일 경우 IllegalArgumentException이 발생한다.")
        void createWithEmptySmallerThenTwoTablesTest(final List<OrderTable> tables) {
            // given
            tables.forEach(table -> orderTableDao.save(table));

            final TableGroup tableGroup = new TableGroup();
            tableGroup.setOrderTables(tables);

            // when & then
            assertThrowsExactly(IllegalArgumentException.class,
                    () -> tableGroupService.create(tableGroup));
        }

        @Test
        @DisplayName("저장되지 않은 OrderTable이 존재할 경우 IllegalArgumentException이 발생한다.")
        void createTableWithNotSavedOrderTable() {
            // given
            final OrderTable savedTable1 = orderTableDao.save(new TableBuilder().build());
            final OrderTable savedTable2 = orderTableDao.save(new TableBuilder().build());
            final OrderTable notSavedTable = new TableBuilder().build();

            final List<OrderTable> tables = List.of(savedTable1, savedTable2, notSavedTable);
            final TableGroup tableGroup = new TableGroupBuilder(tables).build();

            // when & then
            assertThrowsExactly(IllegalArgumentException.class,
                    () -> tableGroupService.create(tableGroup));
        }

        @Test
        @DisplayName("비어있지 않은 OrderTable이 존재할 경우 IllegalArgumentException이 발생한다.")
        void should_throw_when_not_empty_orderTable_exists() {
            // given
            final OrderTable emptyTable = orderTableDao.save(new TableBuilder().build());
            final OrderTable notEmptyTable = orderTableDao.save(new TableBuilder()
                    .setEmpty(false)
                    .build());

            final List<OrderTable> tables = List.of(emptyTable, notEmptyTable);

            final TableGroup tableGroup = new TableGroupBuilder(tables).build();

            // when & then
            assertThrowsExactly(IllegalArgumentException.class,
                    () -> tableGroupService.create(tableGroup));
        }

        @Test
        @DisplayName("이미 테이블 그룹에 속한 테이블이 존재할 경우 IllegalArgumentException이 발생한다.")
        void should_throw_when_already_belong_to_tableGroup() {
            // given
            final OrderTable groupedTable = new TableBuilder()
                    .setTableGroupId(1L)
                    .build();
            final OrderTable table = orderTableDao.save(new TableBuilder().build());

            final TableGroup tableGroup = new TableGroupBuilder(List.of(groupedTable, table)).build();

            // when & then
            assertThrowsExactly(IllegalArgumentException.class,
                    () -> tableGroupService.create(tableGroup));
        }
    }

    @Nested
    @DisplayName("테이블 그룹 해제 테스트")
    class UngroupTest {

        @Test
        @DisplayName("테이블 그룹을 해제하면 그룹에 속하는 모든 테이블의 그룹 id를 null로 설정하고 empty를 true로 설정한다.")
        void ungroupTest() {
            // given
            final TableGroup tableGroup = tableGroupDao.save(new TableGroupBuilder(List.of())
                    .build());

            final OrderTable table1 = orderTableDao.save(new TableBuilder()
                    .setTableGroupId(tableGroup.getId())
                    .setEmpty(true)
                    .build());
            final OrderTable table2 = orderTableDao.save(new TableBuilder()
                    .setTableGroupId(tableGroup.getId())
                    .setEmpty(true)
                    .build());


            // when
            tableGroupService.ungroup(tableGroup.getId());

            // then
            orderTableDao.findById(table1.getId()).ifPresentOrElse(
                    actual -> {
                        assertNull(actual.getTableGroupId());
                        assertFalse(actual.isEmpty());
                    },
                    () -> fail("테이블이 존재하지 않습니다.")
            );

            orderTableDao.findById(table2.getId()).ifPresentOrElse(
                    actual -> {
                        assertNull(actual.getTableGroupId());
                        assertFalse(actual.isEmpty());
                    },
                    () -> fail("테이블이 존재하지 않습니다.")
            );
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"COOKING", "MEAL"})
        @DisplayName("주문 상태가 COOKING 또는 MEAL일 경우 IllegalArgumentException이 발생한다.")
        void should_throw_when_order_status_is_cooking_or_meal(final OrderStatus orderStatus) {
            // given
            final TableGroup tableGroup = tableGroupDao.save(new TableGroupBuilder(List.of())
                    .build());

            final OrderTable table = orderTableDao.save(new TableBuilder()
                    .setTableGroupId(tableGroup.getId())
                    .build());

            orderDao.save(new OrderBuilder()
                    .setOrderTableId(table.getId())
                    .setOrderStatus(orderStatus)
                    .build());

            // when & then
            assertThrowsExactly(IllegalArgumentException.class,
                    () -> tableGroupService.ungroup(tableGroup.getId()));
        }
    }
}