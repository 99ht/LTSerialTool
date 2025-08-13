package indi.lt.serialtool.serialtool.view;

import github.nonoas.jfx.flat.ui.stage.AppStage;

/**
 * 程序通用窗口，设置了一系列通用的样式和参数
 *
 * @author Nonoas
 * @datetime 2022/1/22 22:15
 */
public class BaseStage extends AppStage {

    protected final String TITLE = "WorkTools";

    public BaseStage() {
        setTitle(TITLE);
        // Stage stage = getStage(); // 如果没有 getStage()，请改为直接使用父类暴露的 stage 字段
        // if (stage != null && stage.getScene() != null) {
        //     stage.getScene().getStylesheets().addAll("css/style.css");
        // }
        // addIcons(Collections.singleton(new Image("image/logo.png")));
    }
}
