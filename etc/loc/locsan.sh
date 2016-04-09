#!/bin/bash
grep -rl $'\xEF\xBB\xBF' ../../l10n | while read l; do sed -i '1 s/^\xef\xbb\xbf//'  $l; done
for i in ../../l10n/*.properties; do
    sort ${i} -o ${i}
done
