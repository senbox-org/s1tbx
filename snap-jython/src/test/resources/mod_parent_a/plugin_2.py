started = False
stopped = False


class Activator:
    def start(self):
        global started
        started = True
        from org.esa.snap.core.dataio import ProductIOPlugInManager
        from org.esa.snap.core.gpf import GPF

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

        it = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpis().iterator()
        while it.hasNext():
            plugin = it.next()
            c = plugin.getClass()
            a = plugin.getOperatorAlias()
            print('operator: ' + str(c) + ' alias "' + a + '"')

    def stop(self):
        global stopped
        stopped = True

# Test that import works
import module_x

var = module_x.var
