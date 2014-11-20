package org.jlinda.nest.utils;

/**
* User: pmar@ppolabs.com
* Date: 6/20/11
* Time: 11:16 PM
*/
public class IfgContainer {

    String name;
    public CplxContainer master;
    public CplxContainer slave;
    public String iBandName;
    public String qBandName;

    // TODO: put baselines in the structure
    // Baseline baseline = new Baseline();

    public IfgContainer(String name, CplxContainer master, CplxContainer slave) {
        this.name = name;
        this.master = master;
        this.slave = slave;
    }

}
