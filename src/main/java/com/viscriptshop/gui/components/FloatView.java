package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import dev.vfyjxf.taffy.style.*;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

public class FloatView extends UIElement {
    public final UIElement parent;
    public final UIElement titleBar;
    public final UIElement contentContainer;

    public final Label label;

    //runtime
    //是否还显示在父组件上
    @Getter
    private boolean isShow;
    //contentContainer部分是否被隐藏
    @Getter
    private boolean isHidden;

    public FloatView(UIElement parent, String title) {
        this.parent = parent;
        getLayout().positionType(TaffyPosition.ABSOLUTE);
        getLayout().width(200);
        getLayout().paddingLeft(0);
        getLayout().paddingTop(0);

        this.titleBar = new UIElement();
        this.contentContainer = new UIElement();
        setFocusable(true);
        addEventListener(UIEvents.KEY_DOWN, event -> {
            if (event.keyCode == GLFW.GLFW_KEY_ESCAPE) {
                hide();
                event.stopPropagation();
            }
        });

        Button closeButton = (Button) new Button().setText("x").setOnClick(event -> hide())
                .layout(layout -> {
                    layout.width(10);
                    layout.height(10);
                    layout.marginLeft(5);
                }).style(style -> {
                    style.backgroundTexture(Icons.CLOSE);
                });
        this.titleBar.layout(layout -> {
            layout.widthPercent(100);
            layout.alignItems(AlignItems.CENTER);
            layout.paddingAll(5);
            layout.flexDirection(FlexDirection.ROW);
        }).style(style -> style.backgroundTexture(Sprites.BORDER1_RT1));
        this.label = (Label) new Label()
                .textStyle(style -> style
                        .textAlignVertical(Vertical.CENTER)
                        .textAlignHorizontal(Horizontal.LEFT)
                )
                .setText(title)
                .layout(layout -> layout.flex(1));
        titleBar.addChildren(label, closeButton);

        titleBar.addEventListener(UIEvents.MOUSE_DOWN, event -> titleBar.startDrag(new Vector2f(this.getLayoutX(), this.getLayoutY()), null));
        titleBar.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, event -> {
            if (event.currentElement == titleBar && event.dragHandler.draggingObject instanceof Vector2f initialPos) {
                var newPos = new Vector2f(initialPos).add(event.x - event.dragStartX, event.y - event.dragStartY);
                this.layout(layout -> {
                    layout.left(newPos.x);
                    layout.top(newPos.y);
                });
            }
        });

        titleBar.addEventListener(UIEvents.DOUBLE_CLICK, event -> {
            if (isHidden()) showContainer();
            else hideContainer();
        });

        this.contentContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
            layout.paddingAll(4);
            layout.gapAll(2);
        }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID));

        addChildren(titleBar, contentContainer);
    }

    public FloatView(UIElement parent) {
        this(parent, "");
    }

    public void setTitle(String title) {
        label.setText(title);
    }

    public void show() {
        if (!isShow) {
            isShow = true;
            this.parent.addChildren(this);
        }
        focus();
    }

    public void hide() {
        if (isShow) {
            isShow = false;
            this.parent.removeChild(this);
        }
    }

    public void showContainer() {
        if (isHidden) {
            isHidden = false;
            contentContainer.setDisplay(TaffyDisplay.FLEX);
        }
    }

    public void hideContainer() {
        if (!isHidden) {
            isHidden = true;
            contentContainer.setDisplay(TaffyDisplay.NONE);
        }
    }

    public UIElement createInformation(Component title, Supplier<Component> info) {
        return new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.height(9);
        }).addChildren(
                new Label().setText(title).textStyle(style -> style
                        .adaptiveWidth(true)
                        .textAlignVertical(Vertical.CENTER)),
                new Label().setText(info.get()).textStyle(style -> style
                        .adaptiveWidth(true)
                        .textAlignVertical(Vertical.CENTER)
                        .textAlignHorizontal(Horizontal.RIGHT)).layout(layout -> {
                    layout.flex(1);
                }).addEventListener(UIEvents.TICK, event -> ((Label) event.currentElement).setText(info.get()))
        );
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        adaptPositionToElement(parent);
    }

}
