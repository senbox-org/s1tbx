/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.swing.update;

import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.internal.ModuleImpl;

class ModuleItem implements Comparable<ModuleItem> {

    public static final ModuleItem[] EMPTY_ARRAY = new ModuleItem[0];

    public enum Action {
        NONE,
        INSTALL,
        UPDATE,
        UNINSTALL
    }

    private ModuleImpl module;
    private Action action;
    private Module repositoryModule;

    public ModuleItem(ModuleImpl module) {
        this.module = module;
        this.action = Action.NONE;
        this.repositoryModule = null;
    }

    public ModuleItem(Module repositoryModule) {
        this.module = (ModuleImpl) repositoryModule;
        this.action = Action.NONE;
        this.repositoryModule = repositoryModule;
    }

    public ModuleImpl getModule() {
        return module;
    }

    public void setModule(ModuleImpl module) {
        this.module = module;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Module getRepositoryModule() {
        return repositoryModule;
    }

    public void setRepositoryModule(Module repositoryModule) {
        this.repositoryModule = repositoryModule;
    }

    public String getDisplayName() {
        return ModuleTextFactory.getNameText(this);
    }

    public int compareTo(ModuleItem moduleItem) {
        return getDisplayName().compareToIgnoreCase(moduleItem.getDisplayName());
    }
}
