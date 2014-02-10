class Properties:
    def __init__(self):
        self._names = []

    def load(self, path):
        with open(path, 'r') as file:
            self.load_from_stream(file, path=path)

    def save(self, path):
        with open(path, 'w') as stream:
            self.save_to_stream(stream)

    def save_to_stream(self, stream):
        for name in self._names:
            stream.write(name + ' = ' + self.__dict__[name] + '\n')

    def load_from_stream(self, stream, path=None):
        concat_line = ''
        line_no = 0
        for line in stream:
            line_no += 1
            clean_line = line.strip()
            if (not clean_line) or clean_line.startswith('#'):
                pass
            elif line.endswith('\\'):
                concat_line += line.rstrip('\\')
            else:
                concat_line += line
                self._process_line(concat_line, path, line_no)
                concat_line = ''

    def __len__(self):
        return len(self._names)

    def __getitem__(self, key):
        return self.__dict__[key]

    def __setitem__(self, key, value):
        self.__dict__[key] = value

    def __iter__(self):
        return iter(self._names)

    def iterkeys(self):
        return iter(self._names)

    def add_property(self, name, value):
        if not name in self.__dict__:
            self._names.append(name)
        self.__dict__[name] = value

    def _process_line(self, line, path, line_no):
        i = line.find('=')
        if i == -1:
            raise ValueError(
                ((path + ', ') if path else '') + 'line ' + str(line_no) + ': syntax error: "' + line + '"')
        (name, value) = line.split(sep='=', maxsplit=1)
        name = name.strip()
        value = value.strip()
        self.add_property(name, value)

    @property
    def names(self):
        return self._names[:]





