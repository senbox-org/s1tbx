package com.bc.ceres.swing.update;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 05.04.2007
 * Time: 16:37:28
 * To change this template use File | Settings | File Templates.
 */
class ModuleTableColumnDef {

    private String name;
    private Class<?> type;
    private ModuleValueAccessor accessor;
    // todo - add preferred width
    // todo - add alignment

    public ModuleTableColumnDef(String name, Class<?> type, ModuleValueAccessor accessor) {
        this.name = name;
        this.type = type;
        this.accessor = accessor;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public Object getValue(ModuleItem moduleItem) {
        return accessor.getValue(moduleItem);
    }
}
