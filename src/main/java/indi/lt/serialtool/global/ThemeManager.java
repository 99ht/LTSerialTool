package indi.lt.serialtool.global;

import github.nonoas.jfx.flat.ui.theme.DarkTheme;
import github.nonoas.jfx.flat.ui.theme.LightTheme;
import github.nonoas.jfx.flat.ui.theme.Theme;

import java.util.List;

/**
 * @author Nonoas
 * @date 2025/9/18
 * @since 1.0.0
 */
public class ThemeManager {
    public static List<Theme> getAll() {
        return List.of(new DarkTheme(), new LightTheme());
    }
}
