import argparse
import os
import shutil

import patchtoolslib


def read_patch_list_entries(pl_path):
    return [os.path.split(pl_line.strip()) for pl_line in patchtoolslib.read_txt_as_list(pl_path)]


def read_label_dict(pl_entries, ds_path):
    label_dict = {}
    for pl_entry in pl_entries:
        product_fex_path, patch_name = pl_entry
        label_file_name = product_fex_path + '.txt'
        label_path = os.path.join(ds_path, label_file_name)
        #print(label_path)
        if ((not product_fex_path in label_dict)) and os.path.exists(label_path):
            label_dict[product_fex_path] = patchtoolslib.read_csv_as_dict(label_path, key_index=1)
    return label_dict


def dump_label_dict(label_dict):
    for label_key in sorted(label_dict.keys()):
        print(label_key)
        dump_dict(label_dict[label_key], '    ')


def dump_dict(d, indent=''):
    for k in sorted(d.keys()):
        print(indent, k, ': ', d[k])



def run(pl_file, ds_dir, out_dir):
    pl_entries = read_patch_list_entries(pl_file)
    #print(pl_entries)

    label_dict = read_label_dict(pl_entries, ds_dir)
    #dump_label_dict(label_dict)

    pl_filename = os.path.basename(pl_file)

    if not out_dir or out_dir == '':
        out_dir = patchtoolslib.strip_filename_extension(pl_filename) + '_output'

    if os.path.exists(out_dir):
        print('Output directory ' + out_dir + ' exists an will be be deleted, if you continue. Continue (yes/no): ')
        resp = input().strip().lower()
        if resp == '' or resp == 'no' or resp == 'n':
            print('Cancelled.')
            return

    if os.path.exists(out_dir):
        shutil.rmtree(out_dir)
    os.makedirs(out_dir)

    shutil.copyfile(pl_file, os.path.join(out_dir, pl_filename))

    out_html_file = os.path.join(out_dir, 'content.html')
    with open(out_html_file, 'w') as html_stream:

        html_stream.writelines(['<html>\n',
                                '<head>\n',
                                '    <title></title>\n',
                                '</head>\n',
                                '<body>\n',
                                '<p>pl_file: <a href=\"{0}\">{0}</a></p>\n'.format(pl_filename),
                                '<table>\n'])

        for pl_entry in pl_entries:
            product_fex_path, patch_name = pl_entry
            rel_patch_dir = os.path.join(product_fex_path, patch_name)
            src_patch_path = os.path.normpath(os.path.join(ds_dir, rel_patch_dir))
            if os.path.exists(src_patch_path):

                out_patch_path = os.path.normpath(os.path.join(out_dir, rel_patch_dir))
                shutil.copytree(src_patch_path, out_patch_path)

                files = os.listdir(out_patch_path)
                img_file_names = [file for file in files if str(file).endswith('.png')]
                html_stream.write('    <tr>\n')
                for img_file_name in img_file_names:
                    img_rel_path = os.path.relpath(os.path.join(out_patch_path, img_file_name), start=out_dir)
                    html_stream.write(
                        '        <td><img src=\"{0}\"/><br/><p>{1}</p></td>\n'.format(img_rel_path, img_file_name))
                features_file_name = 'features.txt'
                features_rel_path = os.path.relpath(os.path.join(out_patch_path, features_file_name), start=out_dir)
                html_stream.write('        <td>')
                html_stream.write('{0}<br/>'.format(product_fex_path))
                html_stream.write('Patch: {0}<br/>'.format(patch_name))
                html_stream.write(
                    'Features: <a href=\"{0}\"/>{1}</a><br/>'.format(features_rel_path, features_file_name))

                if product_fex_path in label_dict:
                    patch_dict = label_dict[product_fex_path]
                    if patch_name in patch_dict:
                        row = patch_dict[patch_name]
                        html_stream.write('{0}</p></td>\n'.format(str(row[2:])))
                    else:
                        html_stream.write('<i>No labels found!</i>\n')

                html_stream.write('</td>\n')
                html_stream.write('    </tr>\n')

        html_stream.writelines(['</table>\n',
                                '</body>\n',
                                '</html>\n'])

    print('Output written to ' + out_dir)


parser = argparse.ArgumentParser(description='Display the patches given as a list of sub-paths in file PATCH_LIST.')
parser.add_argument('pl_file', metavar='PATCH_LIST',
                    help='path to the file containing the patches in the form <product>/<patch>')
parser.add_argument('-d', '--ds_dir', default='.',
                    help='path to the dataset directory to retrieve the patches from')
parser.add_argument('-o', '--out_dir', default='',
                    help='path to the output directory, if omitted <PATCH_LIST>_output will be used')
args = parser.parse_args()

run(args.pl_file, args.ds_dir, args.out_dir)
