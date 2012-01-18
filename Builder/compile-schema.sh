#!/bin/sh

xjc -d src -b src/schemas/resources.xjb src/schemas/resources.xsd
xjc -d src src/schemas/translations.xsd
