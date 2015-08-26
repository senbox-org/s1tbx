started = False
stopped = False


def on_snap_start():
    global started
    started = True
    from org.esa.snap.framework.dataio import ProductIOPlugInManager
    from org.esa.snap.framework.gpf import GPF

    it = ProductIOPlugInManager.getInstance().getAllReaderPlugIns()
    while it.hasNext():
        plugin = it.next()
        c = plugin.getClass()
        print('reader plugin: ' + str(c))

    it = ProductIOPlugInManager.getInstance().getAllWriterPlugIns()
    while it.hasNext():
        plugin = it.next()
        c = plugin.getClass()
        print('writer plugin: ' + str(c))

    GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis()
    it = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpis().iterator()
    while it.hasNext():
        plugin = it.next()
        c = plugin.getClass()
        a = plugin.getOperatorAlias()
        print('operator: ' + str(c) + ' alias "' + a + '"')


def on_snap_stop():
    global stopped
    stopped = True


# Test that import works
import module_x
var = module_x.var
