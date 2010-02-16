package com.bc.ceres.swing.update;

import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ModuleState;
import com.bc.ceres.core.runtime.internal.ModuleImpl;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 05.04.2007
 * Time: 16:45:24
 * To change this template use File | Settings | File Templates.
 */
class ModuleTextFactory {
    private static final String NOT_SPECIFIED = "(not specified)";

    static String getText(String s) {
        return s == null ? NOT_SPECIFIED : s;
    }

    static String getActionText(ModuleItem moduleItem) {
        ModuleItem.Action action = moduleItem.getAction();
        String text = "";
        if (action == ModuleItem.Action.NONE) {
            text = "";
        } else if (action == ModuleItem.Action.INSTALL) {
            text = "Install";
        } else if (action == ModuleItem.Action.UPDATE) {
            text = "Update";
        } else if (action == ModuleItem.Action.UNINSTALL) {
            text = "Uninstall";
        }
        return text;
    }

    static String getStateText(ModuleItem moduleItem) {
        ModuleState state = moduleItem.getModule().getState();
        String text = "";
        if (state == ModuleState.NULL) {
            text = "Available";
        } else if (state == ModuleState.RESOLVED) {
            if (moduleItem.getRepositoryModule() != null) {
                text = MessageFormat.format("Resolved, {0} available",
                                            moduleItem.getRepositoryModule().getVersion());
            } else {
                text = "Resolved";
            }
        } else if (state == ModuleState.ACTIVE) {
            if (moduleItem.getRepositoryModule() != null) {
                text = MessageFormat.format("Active, {0} available",
                                            moduleItem.getRepositoryModule().getVersion());
            } else {
                text = "Active";
            }
        } else if (state == ModuleState.INSTALLED) {
            text = "Installed (effective on restart)";
        } else if (state == ModuleState.UNINSTALLED) {
            text = "Uninstalled (effective on restart)";
        }
        return text;
    }

    static String getVersionText(ModuleItem moduleItem) {
        ModuleImpl module = moduleItem.getModule();
        return getVersionText(module);
    }

    static String getVersionText(Module module) {
        return getText(module.getVersion().toString());
    }

    static String getUpdateVersionText(ModuleItem moduleItem) {
        Module repositoryModule = moduleItem.getRepositoryModule();
        return getText(repositoryModule != null ? repositoryModule.getVersion().toString() : null);
    }

    static String getDateText(ModuleItem moduleItem) {
        DateFormat dateInstance = SimpleDateFormat.getDateInstance();
        Module repositoryModule = moduleItem.getRepositoryModule();
        return getText(repositoryModule != null ? dateInstance.format(new Date(repositoryModule.getLastModified())) : null);
    }

    static String getSizeText(ModuleItem moduleItem) {
        long bytes = moduleItem.getRepositoryModule().getContentLength();
        long kilos = Math.round(bytes / 1024.0);
        long megas = Math.round(bytes / (1024.0 * 1024.0));
        return getText(megas > 0 ? (megas + " M") : kilos > 0 ? (kilos + " K") : (bytes + " B"));
    }

    static String getNameText(ModuleItem moduleItem) {
        return getNameText(moduleItem.getModule());
    }

    static String getNameText(Module module) {
        return getText(module.getName() != null ? module.getName() : module.getSymbolicName());
    }

    static String getActionListText(List<ModuleItem> actionList) {
        StringBuilder sb = new StringBuilder(80);
        if (actionList.isEmpty()) {
            sb.append("No actions will be performed.\n");
        } else if (actionList.size() == 1) {
            sb.append("The following action will be performed:\n");
            sb.append("   ");
            sb.append(getActionItemText(actionList.get(0)));
            sb.append('\n');
        } else {
            sb.append("The following actions will be performed:\n");
            for (int i = 0; i < actionList.size(); i++) {
                sb.append("   (");
                sb.append(i + 1);
                sb.append(")   ");
                sb.append(getActionItemText(actionList.get(i)));
                sb.append('\n');
            }
        }
        sb.append('\n');
        return sb.toString();
    }

    static String getActionItemText(ModuleItem actionItem) {
        if (actionItem.getAction() == ModuleItem.Action.UNINSTALL) {
            return MessageFormat.format("Uninstall {0} {1}",
                                        getNameText(actionItem),
                                        getVersionText(actionItem));
        } else if (actionItem.getAction() == ModuleItem.Action.UPDATE) {
            return MessageFormat.format("Update {0} {1} to {2}",
                                        getNameText(actionItem),
                                        getVersionText(actionItem),
                                        getUpdateVersionText(actionItem));
        } else if (actionItem.getAction() == ModuleItem.Action.INSTALL) {
            return MessageFormat.format("Install {0} {1}",
                                        getNameText(actionItem),
                                        getVersionText(actionItem));
        }
        return NOT_SPECIFIED;
    }
}
