package com.example.game.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.game.entity.Role;
import com.example.game.game.processor.PassiveProcessor.AutoPassiveExecutor;
import com.example.game.game.processor.SkillProcessor.SkillExecutor;
import com.example.game.repository.RoleRepository;

public class Room {
    Role[][] roles = new Role[2][5];//这是双方阵营各个角色的位置
    StateProcessor stateProcessor;
    ActionProcessor actionProcessor;
    DamageProcessor damageProcessor;
    AttackProcessor attackProcessor;
    SkillExecutor skillExecutor;
    AutoPassiveExecutor passiveExecutor;
    SurvivalChecker survivalChecker;
    PositionSwapProcessor positionSwapProcessor;
    boolean isGameOver;
    //毒素伤害来源
    Role poison = new Role();
    private String roleInput;

    //阵营buff
    //双方阵营是否有食腐者角色
    List<Role>scavengRoles = new ArrayList<>();
    //是否有易伤buff
    int[][] frost = new int[2][1];
    //是否有跟进buff
    int[][] follow_up = new int[2][1];
    //双方阵营存活角色数量
    int[][] survivalCount = new int[2][1];
    int[][] actionPointer = new int[2][1];//行动指针，0阵营和1阵营各自的行动指针

    public Room(String rolenum, RoleRepository roleRepository) {
        this.roleInput = rolenum;
        String[] idStrings = rolenum.trim().split("\\s+");
        int idx = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 5; j++) {
                if (idx < idStrings.length) {
                    int roleId = Integer.parseInt(idStrings[idx]);
                    Optional<Role> roleOpt = roleRepository.findById(roleId);
                    int row = i;
                    int col = j;
                    roleOpt.ifPresent(role -> {
                        role.setTeamId(row);
                        role.setOrdinalSequence(col);
                        roles[row][col] = role;
                    });
                    idx++;
                }
            }
        }     

        stateProcessor = new StateProcessor();
        actionProcessor = new ActionProcessor();
        attackProcessor = new AttackProcessor();
        skillExecutor = new SkillExecutor("com.example.game.game.processor.SkillProcessor.skills");
        passiveExecutor = new AutoPassiveExecutor("com.example.game.game.processor.PassiveProcessor.passives", this);
        damageProcessor = new DamageProcessor();
        survivalChecker = new SurvivalChecker();
        positionSwapProcessor = new PositionSwapProcessor();
        isGameOver = false;
        //初始化双方阵营存活角色数量
        for (int i = 0; i < 2; i++) {
            survivalCount[i][0] = 5;
        }
    }

    /**
     * 运行完整游戏循环（不清空日志）
     */
    public void runGame() {
        stateProcessor.generateNewLine("Init:" + this.roleInput);
        //执行全部角色的被动效果
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 5; j++) {
                if(roles[i][j].getPassiveId().equals("")){
                    continue;
                }
                Role actor = roles[i][j];
                passiveExecutor.execute(actor, actor.getPassiveId(), this);
            }
        }

        int maxTurns = 200;
        int turnCount = 0;
        while (!isGameOver && turnCount < maxTurns) {
            
                for (int i = 0; i < 2 && !isGameOver; i++) {
                    Role actor = roles[i][actionPointer[i][0]];
                    if (actor.getHp() > 0) {
                        stateProcessor.generateNewLine("Action:" + i + "阵营" + actionPointer[i][0] + "号位在行动");
                        actionProcessor.processAction(actor, this);
                    }
                    if (actionPointer[i][0] < 4) {
                        actionPointer[i][0]++;
                    } else {
                        actionPointer[i][0] = 0;
                    }
                }
            
            turnCount++;
        }
        if (!isGameOver) {
            stateProcessor.generateNewLine("GameOver: 达到最大回合数" + maxTurns + "，强制结束");
        }
        stateProcessor.generateGameOver();
    }

    /**
     * 模拟一局完整的游戏并上传日志（上传后清空缓存）
     */
    public String doTurn() {
        runGame();
        return stateProcessor.uploadText();
    }

    public Role[][] getRoles() {
        return roles;
    }

    public void setRoles(Role[][] roles) {
        this.roles = roles;
    }

    public StateProcessor getStateProcessor() {
        return stateProcessor;
    }

    public void setStateProcessor(StateProcessor stateProcessor) {
        this.stateProcessor = stateProcessor;
    }

    public ActionProcessor getActionProcessor() {
        return actionProcessor;
    }

    public void setActionProcessor(ActionProcessor actionProcessor) {
        this.actionProcessor = actionProcessor;
    }

    public AttackProcessor getAttackProcessor() {
        return attackProcessor;
    }

    public void setAttackProcessor(AttackProcessor attackProcessor) {
        this.attackProcessor = attackProcessor;
    }

    public SkillExecutor getSkillExecutor() {
        return skillExecutor;
    }

    public void setSkillExecutor(SkillExecutor skillExecutor) {
        this.skillExecutor = skillExecutor;
    }

    public AutoPassiveExecutor getPassiveExecutor() {
        return passiveExecutor;
    }
    
    public void setPassiveExecutor(AutoPassiveExecutor passiveExecutor) {
        this.passiveExecutor = passiveExecutor;
    }

    public DamageProcessor getDamageProcessor() {
        return damageProcessor;
    }
    
    public void setDamageProcessor(DamageProcessor damageProcessor) {
        this.damageProcessor = damageProcessor;
    }

    public boolean getGameOver() { 
        return isGameOver;
    }

    public void setGameOver(boolean isGameOver) {
        this.isGameOver = isGameOver;
    }

    public SurvivalChecker getSurvivalChecker() {
        return survivalChecker;
    }
    
    public void setSurvivalChecker(SurvivalChecker survivalChecker) {
        this.survivalChecker = survivalChecker;
    }
    public PositionSwapProcessor getPositionSwapProcessor() {
        return positionSwapProcessor;
    }
    public void setPositionSwapProcessor(PositionSwapProcessor positionSwapProcessor) {
        this.positionSwapProcessor = positionSwapProcessor;
    }

    public List<Role> getScavengRoles() {
        return scavengRoles;
    }
    
    public void addScavengRole(Role role){
        scavengRoles.add(role);
    }

    public int[][] getFrost() {
        return frost;
    }

    public void setFrost(int[][] frost) {
        this.frost = frost;
    }

    public int[][] getFollow_up() {
        return follow_up;
    }
    
    public void setFollow_up(int[][] follow_up) {
        this.follow_up = follow_up;
    }

    public int[][] getSurvivalCount() {
        return survivalCount;
    }
    
    public void setSurvivalCount(int[][] survivalCount) {
        this.survivalCount = survivalCount;
    }

    public String getRoleInput() {
        return roleInput;
    }
    
    public void setRoleInput(String roleInput) {
        this.roleInput = roleInput;
    }

    public Role getPoison() {
        return poison;
    }

    public void setPoison(Role poison) {    
        this.poison = poison;
    }

    public int[][] getActionPointer() {
        return actionPointer;
    }

    public void setActionPointer(int[][] actionPointer) {
        this.actionPointer = actionPointer;
    }
}