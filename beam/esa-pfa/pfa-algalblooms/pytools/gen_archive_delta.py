import argparse
import os
import sys
import numpy
import beampy
import pprint
import datetime

tool_version = '1.0'

jpy = beampy.jpy

ImageManager = jpy.get_type('org.esa.beam.jai.ImageManager')
ImageIO = jpy.get_type('javax.imageio.ImageIO')
File = jpy.get_type('java.io.File')
ProgressMonitor = jpy.get_type('com.bc.ceres.core.ProgressMonitor')

BIG_ENDIAN = (sys.byteorder == 'big')

def run(archive_path, output_path, image_name, rgb_channels):
    print(archive_path, output_path, image_name, rgb_channels)
    product_entries = os.listdir(archive_path)

    if not os.path.exists(output_path): os.mkdir(output_path)

    parameters = dict()
    parameters['archive_path'] = archive_path
    parameters['output_path'] = output_path
    parameters['image_name'] = image_name
    parameters['rgb_channels'] = rgb_channels
    parameters['tool_name'] = __name__
    parameters['tool_version'] = tool_version
    parameters['processing_date'] = str(datetime.datetime.now())
    with open(os.path.join(output_path, 'parameters.json'), 'w') as f:
        pprint.pprint(parameters, stream=f)

    print(product_entries)
    for product_entry in product_entries:
        inp_fex_dir = os.path.join(archive_path, product_entry)
        if not (product_entry.endswith('.fex') and os.path.isdir(inp_fex_dir)):
            continue

        out_fex_dir = os.path.join(output_path, product_entry)
        if not os.path.exists(out_fex_dir): os.mkdir(out_fex_dir)

        patch_entries = os.listdir(inp_fex_dir)
        for patch_entry in patch_entries:
            inp_patch_dir = os.path.join(inp_fex_dir, patch_entry)
            if not (patch_entry.startswith('x') and os.path.isdir(inp_patch_dir)):
                continue

            out_patch_dir = os.path.join(out_fex_dir, patch_entry)
            if not os.path.exists(out_patch_dir): os.mkdir(out_patch_dir)

            inp_dim_file = os.path.join(inp_patch_dir, 'patch.dim')
            print(' reading:', inp_dim_file)
            p = beampy.ProductIO.readProduct(inp_dim_file)

            #b1 = p.getBand(rgb_channels[0])
            #b2 = p.getBand(rgb_channels[1])
            #b3 = p.getBand(rgb_channels[2])

            #rgb_bands = jpy.array(beampy.Band, 3)
            #rgb_bands[0] = p.getBand(rgb_channels[0])
            #rgb_bands[1] = p.getBand(rgb_channels[1])
            #rgb_bands[2] = p.getBand(rgb_channels[2])
            #
            #for band in rgb_bands:
            #    band.getImageInfo(ProgressMonitor.NULL)
            #
            #im = ImageManager.getInstance()
            #imageInfo = im.getImageInfo([b1, b2, b3])
            #image = im.createColoredBandImage(rgb_bands, imageInfo, 0)
            #
            #out_image_path = os.path.join(out_patch_dir, image_name)
            #
            #print(' writing:', out_image_path)
            #ImageIO.write(image, 'PNG', File(out_image_path))

            w = p.getSceneRasterWidth()
            h = p.getSceneRasterHeight()

            flh = numpy.zeros(w * h, dtype=numpy.float32)
            mci = numpy.zeros(w * h, dtype=numpy.float32)

            flhValid = numpy.zeros(w * h, dtype=numpy.uint8)
            mciValid = numpy.zeros(w * h, dtype=numpy.uint8)

            p.getBand('flh').readPixels(0, 0, w, h, flh)
            p.getBand('mci').readPixels(0, 0, w, h, mci)

            p.getBand('flh').readValidMask(0, 0, w, h, flhValid)
            p.getBand('mci').readValidMask(0, 0, w, h, mciValid)

            flh = numpy.where(flhValid != 0, flh, numpy.nan)
            mci = numpy.where(mciValid != 0, mci, numpy.nan)

            #print(patch_entry + ': flhValid = ', flhValid)
            #print(patch_entry + ': flh      = ', flh)

            out_data_dir = os.path.join(out_patch_dir, 'patch.data')
            if not os.path.exists(out_data_dir): os.mkdir(out_data_dir)

            out_flh_img_path = os.path.join(out_data_dir, 'flh')
            out_mci_img_path = os.path.join(out_data_dir, 'mci')

            write_raw_data(out_flh_img_path, flh, w, h)
            write_raw_data(out_mci_img_path, mci, w, h)

            print(' closing:', p.getFileLocation())
            p.dispose()


def write_raw_data(file, data, w, h):
    print(' writing:', file)

    with open(file + '.hdr', 'w') as f:
        f.write('ENVI\n')
        f.write('samples = ' + str(w) + '\n')
        f.write('lines = ' + str(h) + '\n')
        f.write('bands = 1\n')
        f.write('header offset = 0\n')
        f.write('file type = ENVI Standard\n')
        f.write('data type = 4\n')
        f.write('interleave = bsq\n')
        f.write('byte order = 0\n')
        f.write('wavelength = {559.69403}\n')
        f.write('data gain values = {1.0}\n')
        f.write('data offset values = {0.0}\n')

    if BIG_ENDIAN: data.byteswap()
    with open(file + '.img', 'wb') as f:
        data.tofile(f)
    if BIG_ENDIAN: data.byteswap()


parser = argparse.ArgumentParser(description='Generate extra quicklook images for an existing feature ARCHIVE.')
parser.add_argument('archive_path', metavar='ARCHIVE',
                    help='path to an existing feature archive containing *.fex directories')
parser.add_argument('output_path', metavar='OUTPUT',
                    help='path to the output directory in which the extra images will be written')
parser.add_argument('image_name', metavar='IMAGE',
                    help='name of the quicklook images to be written')
parser.add_argument('red_band', metavar='RED',
                    help='the name of the band representing the red channel')
parser.add_argument('green_band', metavar='GREEN',
                    help='the name of the band representing the green channel')
parser.add_argument('blue_band', metavar='BLUE',
                    help='the name of the band representing the blue channel')
parser.add_argument('-d', '--ds_path', default='.',
                    help='path to the dataset directory to retrieve the patches from')
args = parser.parse_args()

run(args.archive_path, args.output_path, args.image_name, [args.red_band, args.green_band, args.blue_band])
