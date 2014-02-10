import unittest
import properties
import io


class PropertyContainer:
    def __init__(self):
        self._names = []

    def __len__(self):
        return len(self._names)

    def __getitem__(self, key):
        return self.attr_container[key]

    def __setitem__(self, key, value):
        if name in self._names:
            self._names[key] = value

    def __iter__(self):
        return iter(self._names)

    def __setattr__(self, key, value):
        self.attr_container[key] = value

    def __getattribute__(self, key):
        if key == 'attr_container':
            return getattr(self, 'attr_container')
        elif key in self.attr_container:
            return self.attr_container[key]
        else:
            return super(PropertyContainer, self).__getattribute__(self, key)


class PropertiesTest(unittest.TestCase):
    def test_obj_nesting(self):
        a = Obj()
        a.x = 5
        self.assertEquals(5, a.x)


    def test_getattribute(self):
        p = properties.Properties()
        p.load_from_stream(['a = 1'])
        self.assertEquals('1', p.a)

    def test_load_retains_occurrence_order_in_file(self):
        p = properties.Properties()
        p.load_from_stream(['x = A', 'a = 1', 'b = 2'])
        self.assertEquals(['x', 'a', 'b'], p.names)
        self.assertEquals('A', p['x'])
        self.assertEquals('1', p['a'])
        self.assertEquals('2', p['b'])

    def test_load_with_comments(self):
        p = properties.Properties()
        p.load_from_stream(['# x = A', 'a = 1', '#b = 2'])
        self.assertEquals(['a'], p.names)
        self.assertEquals('1', p['a'])

    def test_load_with_stringio(self):
        p = properties.Properties()
        p.load_from_stream(io.StringIO('a = 1\nb = 2\n'))
        self.assertEquals(['a', 'b'], p.names)
        self.assertEquals('1', p['a'])
        self.assertEquals('2', p['b'])

    def test_load_with_multiline(self):
        p = properties.Properties()
        p.load_from_stream(['a = \\', '8', 'b = 1'])
        self.assertEquals(['a', 'b'], p.names)
        self.assertEquals('8', p['a'])
        self.assertEquals('1', p['b'])

    def test_load_error(self):
        p = properties.Properties()
        self.assertRaises(ValueError, p.load_from_stream, ['a'])

    def test_save_to_stream(self):
        p = properties.Properties()
        p.add_property('a', 'x')
        p.add_property('z', 'a')
        p.add_property('u', 'z')

        stream = io.StringIO()
        p.save_to_stream(stream)
        s = stream.getvalue()
        stream.close()

        self.assertEquals('a = x\nz = a\nu = z\n', s)


if __name__ == '__main__':
    unittest.main()