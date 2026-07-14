package com.example.game.game;

import com.example.game.entity.Role;

/**
 * 受击处理器
 * 负责处理角色受到的伤害，扣除生命值，并记录战斗日志
 */
public class DamageProcessor {

    // ---------- 构造方法 ----------
    public DamageProcessor() {
    }

    /**
     * 受击处理
     * @param damage   攻击方造成的理论伤害值
     * @param attacker 攻击角色对象
     * @param defender 受击角色对象
     * @return 受击后剩余生命值，如果角色死亡返回 -1
     */
    public void DamageProcess(Role attacker, Room room, Integer damage) {
        //判断是否为群攻
        if(attacker != null && attacker.getBuff_groupAttack() == 1){
            for(Role role : room.getRoles()[attacker.getEnemyTeamId()]){
                if(role.getHp() <= 0){
                    continue;
                }
                DamageProcess(attacker, role, room, damage);
            }
        }
        else if(attacker != null){
            DamageProcess(attacker, room.getRoles()[attacker.getEnemyTeamId()][0], room, damage);
        }

    }

    //单个受击处理函数
    public void DamageProcess(Role attacker, Role defender, Room room, Integer damage) {
        //是否能成为攻击对象
        if(defender.getBuff_notBecomeATargetOfAttack() > 0 && attacker != null && attacker != room.getPoison()){
            if(defender.getOrdinalSequence() != room.getRoles()[defender.getTeamId()].length-1 && attacker.getBuff_groupAttack() == 0 && room.getRoles()[defender.getTeamId()][defender.getOrdinalSequence()+1].getHp() > 0){
                DamageProcess(attacker, room.getRoles()[defender.getTeamId()][defender.getOrdinalSequence()+1], room, damage);
            }
            return;
        }
        //是否有防御Buff
        if(defender.getBuff_damageMinus() > 0 && attacker != null){
            damage = damage - defender.getBuff_damageMinus();
        }
        //是否有代替伤害buff
        if(defender.getBuff_damageReplace() > 0 && attacker != null && damage > 1){
            //通过循环检查受击角色阵营是否有buff_damageReplacePlus
            for(Role role : room.getRoles()[defender.getTeamId()]){
                if(role.getBuff_damageReplacePlus() > 0 && role.getHp() > 0){
                    DamageProcess(null,role, room, 1);
                    damage = 1;
                    break;
                }
            }
        }

        //处理伤害值
        defender.setHp(defender.getHp() - damage);
        if(attacker != null){
            room.getStateProcessor().generateNewLine("attack:" + attacker.getTeamId() + "阵营" + attacker.getOrdinalSequence() + "号位" +"attack "+ defender.getTeamId() + "阵营" + defender.getOrdinalSequence() + "号位 " + damage + "点伤害值");
        }
        //damage:1阵营0号位受到1点伤害
        room.getStateProcessor().generateNewLine("damage:" + defender.getTeamId() + "阵营" + defender.getOrdinalSequence() + "号位受到" + damage + "点伤害");
        //change_hp:1阵营0号位role.hp
        room.getStateProcessor().generateNewLine("change_hp:" + defender.getTeamId() + "阵营" + defender.getOrdinalSequence() + "号位"+defender.getHp());

        //是否有反伤buff
        if(defender.getBuff_damageReturn() > 0 && attacker != null && attacker != room.getPoison()){
            DamageProcess(null,attacker, room, defender.getBuff_damageReturn());
        }

        //是否有易伤buff
        if(room.getFrost()[defender.getTeamId()][0] == 1 && damage > 1 && defender.getOrdinalSequence() == 0 && defender.getHp() > 0 && attacker != null && attacker != room.getPoison()){
            DamageProcess(null,defender, room, 1);
        }

        //查看是否角色是否死亡
        room.getSurvivalChecker().checkSurvival(defender, room);
    }

}