package com.bc.ceres.swing.update;

import com.bc.ceres.core.runtime.ProxyConfig;

public class ConnectionConfigData {

    private String repositoryUrl;
    private boolean proxyUsed;
    private ProxyConfig proxyConfig;

    public ConnectionConfigData() {
        proxyConfig = new ProxyConfig();
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public boolean isProxyUsed() {
        return proxyUsed;
    }

    public void setProxyUsed(boolean proxyUsed) {
        this.proxyUsed = proxyUsed;
    }

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

}
