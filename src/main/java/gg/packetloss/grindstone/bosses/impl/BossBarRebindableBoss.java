package gg.packetloss.grindstone.bosses.impl;

import com.sk89q.commandbook.CommandBook;
import com.skelril.OSBL.bukkit.BukkitBossDeclaration;
import com.skelril.OSBL.bukkit.entity.BukkitBoss;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.InstructionDispatch;
import com.skelril.OSBL.instruction.InstructionResult;
import com.skelril.OSBL.instruction.UnbindInstruction;
import gg.packetloss.grindstone.bosses.detail.BossBarDetail;
import gg.packetloss.grindstone.util.BossBarUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

public class BossBarRebindableBoss<T extends Damageable> extends BukkitBossDeclaration<BossBarDetail> {
    private String boundName;
    private Class<T> clazz;

    public BossBarRebindableBoss(String boundName, Class<T> clazz, Plugin declarer, InstructionDispatch<BossBarDetail> dispatch) {
        super(declarer, dispatch);
        this.clazz = clazz;
        this.boundName = boundName;

        CommandBook.server().getScheduler().runTaskTimer(
                CommandBook.inst(),
                () -> {
                    controlled.values().forEach(this::updateBossBarPlayers);
                },
                20 * 10,
                20 * 2
        );
        CommandBook.server().getScheduler().runTaskTimer(
                CommandBook.inst(),
                () -> {
                    controlled.values().forEach(this::updateBossBarProgress);
                },
                20 * 10,
                5
        );

        unbindInstructions.add(new UnbindInstruction<BossBarDetail>() {
            @Override
            public InstructionResult<BossBarDetail, UnbindInstruction<BossBarDetail>> process(LocalControllable<BossBarDetail> controllable) {
                controllable.getDetail().getBossBar().removeAll();
                return null;
            }
        });
    }

    private void updateBossBarPlayers(LocalControllable<BossBarDetail> controllable) {
        Entity entity = BukkitUtil.getBukkitEntity(controllable.getLocalEntity());
        BossBar bossBar = controllable.getDetail().getBossBar();
        Collection<Player> players = entity.getLocation().getNearbyPlayers(25);
        BossBarUtil.syncWithPlayers(bossBar, players);
    }

    private void updateBossBarProgress(LocalControllable<BossBarDetail> controllable) {
        LivingEntity entity = (LivingEntity) BukkitUtil.getBukkitEntity(controllable.getLocalEntity());
        BossBar bossBar = controllable.getDetail().getBossBar();
        bossBar.setProgress(entity.getHealth() / entity.getMaxHealth());
    }

    private BossBarDetail createDefaultDetail() {
        return new BossBarDetail(Bukkit.createBossBar(boundName, BarColor.RED, BarStyle.SEGMENTED_6));
    }

    public void bind(T entity) {
        bind(new BukkitBoss<>(entity, createDefaultDetail()));
    }

    @Override
    public boolean matchesBind(LocalEntity entity) {
        Entity boss = BukkitUtil.getBukkitEntity(entity);
        return clazz.isInstance(boss) && EntityUtil.nameMatches(boss, boundName);
    }

    @Override
    public LocalControllable<BossBarDetail> tryRebind(LocalEntity entity) {
        var boss = new BukkitBoss<>(clazz.cast(BukkitUtil.getBukkitEntity(entity)), createDefaultDetail());
        silentBind(boss);
        return boss;
    }
}
