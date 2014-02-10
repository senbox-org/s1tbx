import argparse
import os
import csv
import patchtoolslib

def run(pl_path, ds_path):
    out_dir = '.'
    out_path = os.path.join(out_dir, os.path.basename(pl_path) + '.csv')

    pl_entries = patchtoolslib.read_txt_as_list(pl_path)
    #print(pl_entries)

    merged_data_rows = []
    num_rows = 0
    for product_fex_path in pl_entries:

        csv_data_path = os.path.normpath(os.path.join(ds_path, os.path.join(product_fex_path, 'fex-overview.csv')))
        data_rows = patchtoolslib.read_csv_as_list(csv_data_path)

        csv_label_path = os.path.normpath(os.path.join(ds_path, product_fex_path + ".txt"))
        label_rows = patchtoolslib.read_csv_as_list(csv_label_path)

        if len(data_rows) != len(label_rows):
            raise ValueError('Expected ' + str(len(data_rows)) + ' labelled rows, but got ' + str(len(label_rows)))

        for i in range(len(data_rows)):
            data_row = data_rows[i]
            label_row = label_rows[i]
            if i == 0 and num_rows == 0:
                data_row.insert(0, 'product')
                data_row.extend(label_row)
                merged_data_rows.append(data_row)
                num_rows += 1
            elif i > 0:
                data_row.insert(0, product_fex_path)
                data_row.extend(label_row)
                merged_data_rows.append(data_row)
                num_rows += 1


    #print(merged_data_rows)

    with open(out_path, 'w', newline='') as file:
        w = csv.writer(file, delimiter='\t')
        w.writerows(merged_data_rows)

    print('Written ' + out_path)


parser = argparse.ArgumentParser(description='Merge features CSV and labels CSV files for feature-extracted scenes (*.fex) given in PRODUCT_FEX_LIST.')
parser.add_argument('pl_path', metavar='PRODUCT_FEX_LIST',
                    help='path to the file containing the list of feature-extracted scene names (*.fex)')
parser.add_argument('-d', '--ds_path', default='.',
                    help='path to the dataset directory to retrieve the patches from')
args = parser.parse_args()

run(args.pl_path, args.ds_path)
