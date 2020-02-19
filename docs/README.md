# Overview

## Introduction

NASA Astronomy Picture of the Day (APOD) is a free service provided by NASA, offering astronomy-oriented images and videos (along with explanatory descriptions), with new content every day.

**APoD Browser** is an Android-based client for the NASA APOD Service.
It supports selecting available images by date, as well as storing APOD metadata and images locally.

## Intended users

* Visual artists that use astronomical images for inspiration and raw source material.

* Science teachers who want to use interesting photographs in the classroom, to get students excited about astronomy.

* Desktop &amp; mobile device users that like using astronomical images as wallpaper.

### [User stories](user-stories.md) 

## Design documentation

### [Wireframe diagram](wireframe.md)

### [Entity-relationship diagram](erd.md)

## External services

* NASA Astronomy Picture-of-the-Day (APOD)
    
    * Site URL: <https://apod.nasa.gov/apod/astropix.html>
    * API URL: <https://api.nasa.gov/> 
    * Required: Yes, for downloading APOD metadata; as long as the image URLs stay "live", the API is not required for viewing images of previously-download APODs.
     
## Implementation

### [Data definition language (DDL)](ddl.md)

