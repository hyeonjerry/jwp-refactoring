package kitchenpos.application;

import kitchenpos.dao.*;
import kitchenpos.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import support.fixture.*;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderDao orderDao;
    @Autowired
    private MenuDao menuDao;
    @Autowired
    private MenuGroupDao menuGroupDao;
    @Autowired
    private OrderTableDao orderTableDao;
    @Autowired
    private OrderLineItemDao orderLineItemDao;

    @Test
    @DisplayName("모든 주문 목록을 조회한다.")
    void listTest() {
        // given
        final MenuGroup menuGroup = menuGroupDao.save(new MenuGroupBuilder().build());

        final Menu menu = menuDao.save(new MenuBuilder(menuGroup).build());

        final OrderLineItem orderLineItem = new OrderLineItemBuilder(menu.getId(), 1).build();

        final OrderTable table = orderTableDao.save(new TableBuilder()
                .setEmpty(false)
                .build());

        final List<Order> expect = List.of(orderDao
                .save(new OrderBuilder()
                        .setOrderLineItems(List.of(orderLineItem))
                        .setOrderTableId(table.getId())
                        .build()
                )
        );

        // when
        final List<Order> actual = orderService.list();

        // then
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id", "orderLineItems")
                .isEqualTo(expect);
    }

    @Nested
    @DisplayName("주문을 생성한다.")
    class CreateTest {

        @Test
        @DisplayName("생성된 주문의 상태는 COOKING이고 OrderLineItem의 OrderId는 생성된 주문의 id이다.")
        void createOrderTest() {
            // given
            final MenuGroup menuGroup = menuGroupDao.save(new MenuGroupBuilder().build());

            final Menu menu = menuDao.save(new MenuBuilder(menuGroup).build());

            final OrderLineItem orderLineItem = new OrderLineItemBuilder(menu.getId(), 1).build();

            final OrderTable table = orderTableDao.save(new TableBuilder()
                    .setEmpty(false)
                    .build());

            final Order order = new OrderBuilder()
                    .setOrderLineItems(List.of(orderLineItem))
                    .setOrderTableId(table.getId())
                    .build();

            // when
            final Order savedOrder = orderService.create(order);

            // then
            assertEquals(OrderStatus.COOKING.name(), savedOrder.getOrderStatus());

            final Long orderLineItemId = savedOrder.getOrderLineItems().get(0).getSeq();
            orderLineItemDao.findById(orderLineItemId)
                    .ifPresentOrElse(
                            actual -> assertEquals(savedOrder.getId(), actual.getOrderId()),
                            () -> fail("OrderLineItem이 존재하지 않습니다.")
                    );
        }

        @Test
        @DisplayName("OrderLineItem이 비어있을 경우 IllegalArgumentException이 발생한다.")
        void should_throw_when_OrderLineItem_is_empty() {
            // given
            final OrderTable table = orderTableDao.save(new TableBuilder()
                    .setEmpty(false)
                    .build());

            final Order order = new OrderBuilder()
                    .setOrderLineItems(Collections.emptyList())
                    .setOrderTableId(table.getId())
                    .build();

            // when & then
            assertThrowsExactly(IllegalArgumentException.class,
                    () -> orderService.create(order));
        }

        @Test
        @DisplayName("저장되지 않은 OrderLineItem이 존재할 경우 IllegalArgumentException이 발생한다.")
        void should_throw_when_OrderLineItem_is_not_saved() {
            // given
            final long notExistsMenuId = -1L;
            final OrderLineItem orderLineItem = new OrderLineItemBuilder(notExistsMenuId, 1).build();

            final OrderTable table = orderTableDao.save(new TableBuilder()
                    .setEmpty(false)
                    .build());

            final Order order = new OrderBuilder()
                    .setOrderLineItems(List.of(orderLineItem))
                    .setOrderTableId(table.getId())
                    .build();

            // when & then
            assertThrowsExactly(IllegalArgumentException.class,
                    () -> orderService.create(order));
        }

        @Test
        @DisplayName("주문 테이블이 존재하지 않을 경우 IllegalArgumentException이 발생한다.")
        void should_throw_when_OrderTable_is_not_saved() {
            // given
            final MenuGroup menuGroup = menuGroupDao.save(new MenuGroupBuilder().build());

            final Menu menu = menuDao.save(new MenuBuilder(menuGroup).build());

            final OrderLineItem orderLineItem = new OrderLineItemBuilder(menu.getId(), 1).build();

            final long notExistsOrderTableId = -1L;
            final Order order = new OrderBuilder()
                    .setOrderLineItems(List.of(orderLineItem))
                    .setOrderTableId(notExistsOrderTableId)
                    .build();

            // when & then
            assertThrowsExactly(IllegalArgumentException.class,
                    () -> orderService.create(order));
        }

        @Test
        @DisplayName("주문 테이블이 비어있는 경우 IllegalArgumentException이 발생한다.")
        void should_throw_when_OrderTable_is_empty() {
            // given
            final MenuGroup menuGroup = menuGroupDao.save(new MenuGroupBuilder().build());

            final Menu menu = menuDao.save(new MenuBuilder(menuGroup).build());

            final OrderLineItem orderLineItem = new OrderLineItemBuilder(menu.getId(), 1).build();

            final OrderTable table = orderTableDao.save(new TableBuilder()
                    .setEmpty(true)
                    .build());

            final Order order = new OrderBuilder()
                    .setOrderLineItems(List.of(orderLineItem))
                    .setOrderTableId(table.getId())
                    .build();

            // when & then
            assertThrowsExactly(IllegalArgumentException.class,
                    () -> orderService.create(order));
        }
    }

    @Nested
    @DisplayName("주문 상태 변경 테스트")
    class ChangeOrderStatusTest {

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"MEAL", "COMPLETION"})
        @DisplayName("주문 상태를 변경한다.")
        void changeOrderStatusTest(final OrderStatus orderStatus) {
            // given
            final MenuGroup menuGroup = menuGroupDao.save(new MenuGroupBuilder().build());

            final Menu menu = menuDao.save(new MenuBuilder(menuGroup).build());

            final OrderLineItem orderLineItem = new OrderLineItemBuilder(menu.getId(), 1).build();

            final OrderTable table = orderTableDao.save(new TableBuilder()
                    .setEmpty(false)
                    .build());

            final Order order = orderDao.save(new OrderBuilder()
                    .setOrderLineItems(List.of(orderLineItem))
                    .setOrderTableId(table.getId())
                    .build());

            // when
            order.setOrderStatus(orderStatus.name());
            orderService.changeOrderStatus(order.getId(), order);

            // then
            orderDao.findById(order.getId())
                    .ifPresentOrElse(
                            actual -> assertEquals(orderStatus.name(), actual.getOrderStatus()),
                            () -> fail("Order가 존재하지 않습니다.")
                    );
        }

        @Test
        @DisplayName("완료된 주문의 상태를 변경할 경우 IllegalArgumentException이 발생한다.")
        void should_throw_when_change_orderStatus_completion() {
            // given
            final MenuGroup menuGroup = menuGroupDao.save(new MenuGroupBuilder().build());

            final Menu menu = menuDao.save(new MenuBuilder(menuGroup).build());

            final OrderLineItem orderLineItem = new OrderLineItemBuilder(menu.getId(), 1).build();

            final OrderTable table = orderTableDao.save(new TableBuilder()
                    .setEmpty(false)
                    .build());

            final Order order = orderDao.save(new OrderBuilder()
                    .setOrderLineItems(List.of(orderLineItem))
                    .setOrderTableId(table.getId())
                    .setOrderStatus(OrderStatus.COMPLETION)
                    .build());

            // when & then
            assertThrowsExactly(IllegalArgumentException.class,
                    () -> orderService.changeOrderStatus(order.getId(), order));
        }
    }
}
