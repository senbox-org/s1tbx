started = False
stopped = False


class Activator:
    def start(self):
        global started
        started = True

    def stop(self):
        global stopped
        stopped = True
