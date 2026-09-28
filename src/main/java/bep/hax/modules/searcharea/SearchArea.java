package bep.hax.modules.searcharea;
import bep.hax.Bep;
import bep.hax.modules.searcharea.modes.Rectangle;
import bep.hax.modules.searcharea.modes.Spiral;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
public class SearchArea extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<SearchAreaModes> chunkLoadMode = sgGeneral.add(new EnumSetting.Builder<SearchAreaModes>()
        .name("mode")
        .description("The mode chunks are loaded.")
        .defaultValue(SearchAreaModes.Rectangle)
        .onModuleActivated(chunkMode -> onModeChanged(chunkMode.get()))
        .onChanged(this::onModeChanged)
        .build()
    );
    public final Setting<BlockPos> startPos = sgGeneral.add(new BlockPosSetting.Builder()
        .name("start-position")
        .description("The coordinates to start the rectangle at. Y Pos is ignored")
        .defaultValue(new BlockPos(0,0,0))
        .visible(() -> chunkLoadMode.get() == SearchAreaModes.Rectangle)
        .build()
    );
    public final Setting<BlockPos> targetPos = sgGeneral.add(new BlockPosSetting.Builder()
        .name("end-position")
        .description("The coordinates to end the rectangle at. Y Pos is ignored")
        .defaultValue(new BlockPos(0,0,0))
        .visible(() -> chunkLoadMode.get() == SearchAreaModes.Rectangle)
        .build()
    );
    public final Setting<Integer> rowGap = sgGeneral.add(new IntSetting.Builder()
        .name("path-gap")
        .description("The amount of chunks to space between each chunk path.")
        .defaultValue(12)
        .min(1)
        .sliderRange(0, 32)
        .build()
    );
    public final Setting<String> saveLocation = sgGeneral.add(new StringSetting.Builder()
        .name("save-name")
        .description("The name to use for the folder that saves data, if you leave it blank, no data will be saved.")
        .defaultValue("")
        .build()
    );
    public final Setting<Boolean> disconnectOnCompletion = sgGeneral.add(new BoolSetting.Builder()
        .name("disconnect-on-completion")
        .description("Whether to disconnect after the path is complete. This will turn autoreconnect off when disconnecting.")
        .defaultValue(false)
        .visible(() -> chunkLoadMode.get() == SearchAreaModes.Rectangle)
        .build()
    );
    public SearchArea() {
        super(Bep.STASH, "search-area", "Either loads chunks in a rectangle to a certain point from you, or spirals endlessly from you. Useful with Stash Finder or other map saving mods.");
    }
    private SearchAreaMode currentMode = new Rectangle();
    @Override
    public WWidget getWidget(GuiTheme theme)
    {
        WVerticalList list = theme.verticalList();
        WButton clear = list.add(theme.button("Clear Currently Selected")).widget();
        clear.action = () -> currentMode.clear();
        WButton clearAll = list.add(theme.button("Clear All")).widget();
        clearAll.action = () -> currentMode.clearAll();
        return list;
    }
    private boolean modeActivated = false;   // 记录当前 mode 是否已 onActivate，避免重复激活
    @Override
    public void onActivate() {
        currentMode.onActivate();
        modeActivated = true;
    }
    @Override
    public void onDeactivate()
    {
        currentMode.onDeactivate();
        modeActivated = false;
    }
    @EventHandler
    private void onTick(TickEvent.Post event)
    {
        currentMode.onTick();
    }
    private void onModeChanged(SearchAreaModes mode) {
        // 修复：运行中切换模式时旧实现只换了实例、不调 onActivate()，导致子模式内部状态（Spiral.pd 等）为 null，
        // 下一 tick 直接 NPE 崩游戏。这里按激活状态正确交接。
        if (modeActivated) currentMode.onDeactivate();
        switch (mode) {
            case Rectangle -> currentMode = new Rectangle();
            case Spiral -> currentMode = new Spiral();
        }
        if (modeActivated) currentMode.onActivate();
    }
    public enum WebhookSettings
    {
        Off,
        LogChat,
        LogStashes,
        LogBoth
    }
}