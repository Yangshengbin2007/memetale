package main.java.game;

public class SceneManager {
    // 计划中将添加：
    // - 当前场景引用，例如：private Scene currentScene;
    // - 可选的场景栈，例如：private Deque<Scene> sceneStack;
    // - 场景工厂/注册表，例如：private Map<SceneId, Supplier<Scene>> sceneFactory;
    //
    // 对外方法计划：
    // - 注册场景：registerScene(...)
    // - 切换场景：switchTo(...)
    // - 压栈/出栈：push(...), pop()
    // - 游戏循环调用的入口：update(...), render(...), handleInput(...)
    //
    // 具体实现将在完成整体设计与接口定义后再编写。

    // ========= 包 / 文件夹结构规划 =========
    // 计划中的核心包结构（对应磁盘上的文件夹）：
    //
    // core   : 游戏核心 - Game 主循环、SceneManager、Scene 接口等
    // scenes : 具体场景 - MainMenuScene, GamePlayScene, PauseScene, SettingsScene, GameOverScene
    // input  : 输入管理 - InputManager, 键盘/鼠标适配封装
    // assets : 资源管理 - AssetManager, AudioManager, 配置加载
    // model  : 游戏领域模型 - 谱面/音符/玩家/分数等
    // ui     : UI 组件 - 按钮、菜单组件、HUD 等
    // util   : 工具类 - 数学、时间工具、日志封装等
    //
    // 未来计划：
    // - 将本类移动到 main.java.game.core 包：
    //     package main.java.game.core;
    //     // filepath: /.../src/main/java/game/core/SceneManager.java
    // - 在 /src/main/java/game 下手动创建上述子文件夹（core, scenes, input, assets, model, ui, util）
    // - 新建/迁移对应类到各自包下，并同步修改 package 声明
}
