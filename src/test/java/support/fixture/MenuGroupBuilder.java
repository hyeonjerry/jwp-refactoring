package support.fixture;

import kitchenpos.domain.MenuGroup;

public class MenuGroupBuilder {

    private static int sequence = 1;

    private final MenuGroup menuGroup;

    public MenuGroupBuilder() {
        this.menuGroup = new MenuGroup();
        menuGroup.setName("메뉴그룹" + sequence);

        sequence++;
    }

    public MenuGroupBuilder setName(final String name) {
        menuGroup.setName(name);
        return this;
    }

    public MenuGroup build() {
        return menuGroup;
    }
}