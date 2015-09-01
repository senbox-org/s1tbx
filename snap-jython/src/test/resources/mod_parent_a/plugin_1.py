started = False
stopped = False


def on_snap_start():
    global started
    started = True


def on_snap_stop():
    global stopped
    stopped = True
