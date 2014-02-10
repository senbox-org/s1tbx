import csv


def read_txt_as_list(path):
    with open(path, 'r') as file:
        return [line.strip() for line in file]


def read_csv_as_dict(path, key_index=0):
    with open(path, 'r', newline='') as file:
        rows = csv.reader(file, delimiter='\t')
        row_dict = {}
        for row in rows:
            key = row[key_index]
            row_dict[key] = row
        return row_dict


def read_csv_as_list(path):
    with open(path, 'r', newline='') as file:
        rows = csv.reader(file, delimiter='\t')
        return [row for row in rows]


def strip_filename_extension(filename):
    pos = filename.rfind('.')
    return filename if pos <= 0 else filename[:pos]

