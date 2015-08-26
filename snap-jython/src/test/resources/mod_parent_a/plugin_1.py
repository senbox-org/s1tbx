
started = False
stopped = False

print("started = " + str(started))

def on_snap_start():
    started = True
    print("started = " + str(started))


def on_snap_stop():
    stopped = True
