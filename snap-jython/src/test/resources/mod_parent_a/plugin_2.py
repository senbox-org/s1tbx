import module_x

started = False
stopped = False


def on_snap_start():
    started = True


def on_snap_stop():
    stopped = True

