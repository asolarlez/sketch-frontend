#!/usr/bin/env python3.0

import random, optparse
import xml.dom.minidom as xml

skresolution = {
    4: [(0, 1), (2, 3), (1, 3), (0, 2), (1, 2)],
    5: [(2, 4), (0, 2), (1, 3), (2, 4), (2, 3), (0, 1), (1, 2), (2, 4),
        (3, 4)],
    6: [(2, 3), (0, 1), (4, 5), (1, 5), (1, 2), (0, 4), (3, 4), (0, 1),
        (4, 5), (2, 3), (1, 2), (3, 4)]
    }

def batcher_sort(array, cas_ops):
    for i, j in cas_ops:
        if array[i] > array[j]:
            tmp = array[j]
            array[j] = array[i]
            array[i] = tmp
    return array

def sort_rand(length, ntrials):
    original_resoltion = skresolution[length][:]

    print("batcher sort of length %d taking %d steps."
          %(length, len(original_resoltion)))
    array = [random.randint(0, 10000) for i in range(length)]
    print("bubble sort would have taken %d steps."
          %((length * (length - 1)) / 2))
    print(array)
    print(batcher_sort(array[:], original_resoltion))

    # run a lot of trials
    for trial_idx in range(ntrials):
        array = [random.randint(0, 10000) for i in range(length)]
        sorted = batcher_sort(array[:], original_resoltion)
        assert(sum(array) == sum(sorted))
        for i in range(length - 1):
            assert(sorted[i] <= sorted[i + 1])
    print("%d trials succesfully completed, all sorted correctly" %(ntrials))

    # attempt to remove one of the operations and see if affects accuracy
    for i in range(len(original_resoltion)):
        subarr = original_resoltion[:i] + original_resoltion[(i + 1):]
        failed = False
        for trial_idx in range(ntrials):
            array = [random.randint(0, 1000) for i in range(length)]
            sorted = batcher_sort(array[:], subarr)
            assert not failed
            for i in range(length - 1):
                if sorted[i] > sorted[i + 1]:
                    failed = True
                    break
            if failed:
                break
        if not failed:
            print("redundant operation, reduced ops =", subarr)
            return
    print("no operations could be removed without changing accuracy.")

def visualize(length):
    original_resolution = skresolution[length][:]
    doc = xml.Document()
    html = xml.Element("html")
    table = html.appendChild(xml.Element("body")).appendChild(xml.Element("table"))
    table.setAttribute("border", "1")
    for i, j in original_resolution:
        row = table.appendChild(xml.Element("tr"))
        for a in range(length):
            color = ["white", "green", "blue"][int(a == i) + 2 * (a == j)]
            cell = row.appendChild(xml.Element("td"))
            cell.setAttribute("style", "background-color: %s; "
                    "width: 18px; height: 10px" %(color))
            cell.appendChild(doc.createTextNode("_"))
        spacer_row = table.appendChild(xml.Element("tr"))
        spacer_row.setAttribute("style", "height: 10px;")
    # fixme - text node converts "&nbsp;" to "&amp;nbsp;"
    print(html.toprettyxml().replace("\t", " " * 4).replace("_", "&nbsp;"))

if __name__ == "__main__":
    cmdopts = optparse.OptionParser()
    cmdopts.add_option("--veclen", type="int",
                       help="length of vector to sort. MANDATORY.")
    cmdopts.add_option("--verifylen", type="int", default=10000,
                       help="number of vectors to verify")
    cmdopts.add_option("--visualize", action="store_true", default=False,
                       help="output HTML visualization for a given veclen")
    (options, args) = cmdopts.parse_args()
    if not options.veclen:
        cmdopts.error("veclen argument is mandatory")
    if options.visualize:
        visualize(options.veclen)
    else:
        sort_rand(options.veclen, options.verifylen)
