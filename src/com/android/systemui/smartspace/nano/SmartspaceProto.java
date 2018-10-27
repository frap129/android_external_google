package com.android.systemui.smartspace.nano;

import com.android.systemui.plugins.R;
import com.google.protobuf.nano.CodedInputByteBufferNano;
import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.InternalNano;
import com.google.protobuf.nano.MessageNano;
import com.google.protobuf.nano.WireFormatNano;

import java.io.IOException;
import java.util.Arrays;

public interface SmartspaceProto {

    public static final class CardWrapper extends MessageNano {
        public SmartspaceUpdate.SmartspaceCard card;
        public long gsaUpdateTime;
        public int gsaVersionCode;
        public byte[] icon;
        public boolean isIconGrayscale;
        public long publishTime;

        public CardWrapper() {
            clear();
        }

        public CardWrapper clear() {
            this.card = null;
            this.publishTime = 0;
            this.gsaUpdateTime = 0;
            this.gsaVersionCode = 0;
            this.icon = WireFormatNano.EMPTY_BYTES;
            this.isIconGrayscale = false;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.card != null) {
                output.writeMessage(1, this.card);
            }
            if (this.publishTime != 0) {
                output.writeInt64(2, this.publishTime);
            }
            if (this.gsaUpdateTime != 0) {
                output.writeInt64(3, this.gsaUpdateTime);
            }
            if (this.gsaVersionCode != 0) {
                output.writeInt32(4, this.gsaVersionCode);
            }
            if (!Arrays.equals(this.icon, WireFormatNano.EMPTY_BYTES)) {
                output.writeBytes(5, this.icon);
            }
            if (this.isIconGrayscale) {
                output.writeBool(6, this.isIconGrayscale);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.card != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(1, this.card);
            }
            if (this.publishTime != 0) {
                size += CodedOutputByteBufferNano.computeInt64Size(2, this.publishTime);
            }
            if (this.gsaUpdateTime != 0) {
                size += CodedOutputByteBufferNano.computeInt64Size(3, this.gsaUpdateTime);
            }
            if (this.gsaVersionCode != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(4, this.gsaVersionCode);
            }
            if (!Arrays.equals(this.icon, WireFormatNano.EMPTY_BYTES)) {
                size += CodedOutputByteBufferNano.computeBytesSize(5, this.icon);
            }
            if (this.isIconGrayscale) {
                return size + CodedOutputByteBufferNano.computeBoolSize(6, this.isIconGrayscale);
            }
            return size;
        }

        public CardWrapper mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 10) {
                    if (this.card == null) {
                        this.card = new SmartspaceUpdate.SmartspaceCard();
                    }
                    input.readMessage(this.card);
                } else if (tag == 16) {
                    this.publishTime = input.readInt64();
                } else if (tag == 24) {
                    this.gsaUpdateTime = input.readInt64();
                } else if (tag == 32) {
                    this.gsaVersionCode = input.readInt32();
                } else if (tag == 42) {
                    this.icon = input.readBytes();
                } else if (tag == 48) {
                    this.isIconGrayscale = input.readBool();
                } else if (!WireFormatNano.parseUnknownField(input, tag)) {
                    return this;
                }
            }
        }
    }

    public static final class SmartspaceUpdate extends MessageNano {
        public SmartspaceCard[] card;

        public static final class SmartspaceCard extends MessageNano {
            private static volatile SmartspaceCard[] _emptyArray;
            public int cardId;
            public int cardPriority;
            public int cardType;
            public Message duringEvent;
            public long eventDurationMillis;
            public long eventTimeMillis;
            public ExpiryCriteria expiryCriteria;
            public Image icon;
            public Message postEvent;
            public Message preEvent;
            public boolean shouldDiscard;
            public TapAction tapAction;
            public long updateTimeMillis;

            public static final class ExpiryCriteria extends MessageNano {
                public long expirationTimeMillis;
                public int maxImpressions;

                public ExpiryCriteria() {
                    clear();
                }

                public ExpiryCriteria clear() {
                    this.expirationTimeMillis = 0;
                    this.maxImpressions = 0;
                    this.cachedSize = -1;
                    return this;
                }

                public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                    if (this.expirationTimeMillis != 0) {
                        output.writeInt64(1, this.expirationTimeMillis);
                    }
                    if (this.maxImpressions != 0) {
                        output.writeInt32(2, this.maxImpressions);
                    }
                    super.writeTo(output);
                }

                protected int computeSerializedSize() {
                    int size = super.computeSerializedSize();
                    if (this.expirationTimeMillis != 0) {
                        size += CodedOutputByteBufferNano.computeInt64Size(1, this.expirationTimeMillis);
                    }
                    if (this.maxImpressions != 0) {
                        return size + CodedOutputByteBufferNano.computeInt32Size(2, this.maxImpressions);
                    }
                    return size;
                }

                public ExpiryCriteria mergeFrom(CodedInputByteBufferNano input) throws IOException {
                    while (true) {
                        int tag = input.readTag();
                        if (tag == 0) {
                            return this;
                        }
                        if (tag == 8) {
                            this.expirationTimeMillis = input.readInt64();
                        } else if (tag == 16) {
                            this.maxImpressions = input.readInt32();
                        } else if (!WireFormatNano.parseUnknownField(input, tag)) {
                            return this;
                        }
                    }
                }
            }

            public static final class Image extends MessageNano {
                public String gsaResourceName;
                public String key;
                public String uri;

                public Image() {
                    clear();
                }

                public Image clear() {
                    this.key = "";
                    this.gsaResourceName = "";
                    this.uri = "";
                    this.cachedSize = -1;
                    return this;
                }

                public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                    if (!this.key.equals("")) {
                        output.writeString(1, this.key);
                    }
                    if (!this.gsaResourceName.equals("")) {
                        output.writeString(2, this.gsaResourceName);
                    }
                    if (!this.uri.equals("")) {
                        output.writeString(3, this.uri);
                    }
                    super.writeTo(output);
                }

                protected int computeSerializedSize() {
                    int size = super.computeSerializedSize();
                    if (!this.key.equals("")) {
                        size += CodedOutputByteBufferNano.computeStringSize(1, this.key);
                    }
                    if (!this.gsaResourceName.equals("")) {
                        size += CodedOutputByteBufferNano.computeStringSize(2, this.gsaResourceName);
                    }
                    if (this.uri.equals("")) {
                        return size;
                    }
                    return size + CodedOutputByteBufferNano.computeStringSize(3, this.uri);
                }

                public Image mergeFrom(CodedInputByteBufferNano input) throws IOException {
                    while (true) {
                        int tag = input.readTag();
                        if (tag == 0) {
                            return this;
                        }
                        if (tag == 10) {
                            this.key = input.readString();
                        } else if (tag == 18) {
                            this.gsaResourceName = input.readString();
                        } else if (tag == 26) {
                            this.uri = input.readString();
                        } else if (!WireFormatNano.parseUnknownField(input, tag)) {
                            return this;
                        }
                    }
                }
            }

            public static final class Message extends MessageNano {
                public FormattedText subtitle;
                public FormattedText title;

                public static final class FormattedText extends MessageNano {
                    public FormatParam[] formatParam;
                    public String text;
                    public int truncateLocation;

                    public static final class FormatParam extends MessageNano {
                        private static volatile FormatParam[] _emptyArray;
                        public int formatParamArgs;
                        public String text;
                        public int truncateLocation;
                        public boolean updateTimeLocally;

                        public static FormatParam[] emptyArray() {
                            if (_emptyArray == null) {
                                synchronized (InternalNano.LAZY_INIT_LOCK) {
                                    if (_emptyArray == null) {
                                        _emptyArray = new FormatParam[0];
                                    }
                                }
                            }
                            return _emptyArray;
                        }

                        public FormatParam() {
                            clear();
                        }

                        public FormatParam clear() {
                            this.text = "";
                            this.truncateLocation = 0;
                            this.formatParamArgs = 0;
                            this.updateTimeLocally = false;
                            this.cachedSize = -1;
                            return this;
                        }

                        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                            if (!this.text.equals("")) {
                                output.writeString(1, this.text);
                            }
                            if (this.truncateLocation != 0) {
                                output.writeInt32(2, this.truncateLocation);
                            }
                            if (this.formatParamArgs != 0) {
                                output.writeInt32(3, this.formatParamArgs);
                            }
                            if (this.updateTimeLocally) {
                                output.writeBool(4, this.updateTimeLocally);
                            }
                            super.writeTo(output);
                        }

                        protected int computeSerializedSize() {
                            int size = super.computeSerializedSize();
                            if (!this.text.equals("")) {
                                size += CodedOutputByteBufferNano.computeStringSize(1, this.text);
                            }
                            if (this.truncateLocation != 0) {
                                size += CodedOutputByteBufferNano.computeInt32Size(2, this.truncateLocation);
                            }
                            if (this.formatParamArgs != 0) {
                                size += CodedOutputByteBufferNano.computeInt32Size(3, this.formatParamArgs);
                            }
                            if (this.updateTimeLocally) {
                                return size + CodedOutputByteBufferNano.computeBoolSize(4, this.updateTimeLocally);
                            }
                            return size;
                        }

                        public FormatParam mergeFrom(CodedInputByteBufferNano input) throws IOException {
                            while (true) {
                                int tag = input.readTag();
                                if (tag == 0) {
                                    return this;
                                }
                                if (tag != 10) {
                                    int value;
                                    if (tag != 16) {
                                        if (tag == 24) {
                                            value = input.readInt32();
                                            switch (value) {
                                                case 0:
                                                case 1:
                                                case 2:
                                                case 3:
                                                    this.formatParamArgs = value;
                                                    break;
                                                default:
                                                    break;
                                            }
                                        } else if (tag == 32) {
                                            this.updateTimeLocally = input.readBool();
                                        } else if (!WireFormatNano.parseUnknownField(input, tag)) {
                                            return this;
                                        }
                                    } else {
                                        value = input.readInt32();
                                        switch (value) {
                                            case 0:
                                            case 1:
                                            case 2:
                                            case 3:
                                                this.truncateLocation = value;
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                }
                                this.text = input.readString();
                            }
                        }
                    }

                    public FormattedText() {
                        clear();
                    }

                    public FormattedText clear() {
                        this.text = "";
                        this.truncateLocation = 0;
                        this.formatParam = FormatParam.emptyArray();
                        this.cachedSize = -1;
                        return this;
                    }

                    public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                        if (!this.text.equals("")) {
                            output.writeString(1, this.text);
                        }
                        if (this.truncateLocation != 0) {
                            output.writeInt32(2, this.truncateLocation);
                        }
                        if (this.formatParam != null && this.formatParam.length > 0) {
                            for (FormatParam element : this.formatParam) {
                                if (element != null) {
                                    output.writeMessage(3, element);
                                }
                            }
                        }
                        super.writeTo(output);
                    }

                    protected int computeSerializedSize() {
                        int size = super.computeSerializedSize();
                        if (!this.text.equals("")) {
                            size += CodedOutputByteBufferNano.computeStringSize(1, this.text);
                        }
                        if (this.truncateLocation != 0) {
                            size += CodedOutputByteBufferNano.computeInt32Size(2, this.truncateLocation);
                        }
                        if (this.formatParam != null && this.formatParam.length > 0) {
                            for (FormatParam element : this.formatParam) {
                                if (element != null) {
                                    size += CodedOutputByteBufferNano.computeMessageSize(3, element);
                                }
                            }
                        }
                        return size;
                    }

                    public FormattedText mergeFrom(CodedInputByteBufferNano input) throws IOException {
                        while (true) {
                            int tag = input.readTag();
                            if (tag == 0) {
                                return this;
                            }
                            if (tag != 10) {
                                int value;
                                if (tag == 16) {
                                    value = input.readInt32();
                                    switch (value) {
                                        case 0:
                                        case 1:
                                        case 2:
                                        case 3:
                                            this.truncateLocation = value;
                                            break;
                                        default:
                                            break;
                                    }
                                } else if (tag == 26) {
                                    value = WireFormatNano.getRepeatedFieldArrayLength(input, 26);
                                    int i = this.formatParam == null ? 0 : this.formatParam.length;
                                    FormatParam[] newArray = new FormatParam[(i + value)];
                                    if (i != 0) {
                                        System.arraycopy(this.formatParam, 0, newArray, 0, i);
                                    }
                                    while (i < newArray.length - 1) {
                                        newArray[i] = new FormatParam();
                                        input.readMessage(newArray[i]);
                                        input.readTag();
                                        i++;
                                    }
                                    newArray[i] = new FormatParam();
                                    input.readMessage(newArray[i]);
                                    this.formatParam = newArray;
                                } else if (!WireFormatNano.parseUnknownField(input, tag)) {
                                    return this;
                                }
                            } else {
                                this.text = input.readString();
                            }
                        }
                    }
                }

                public Message() {
                    clear();
                }

                public Message clear() {
                    this.title = null;
                    this.subtitle = null;
                    this.cachedSize = -1;
                    return this;
                }

                public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                    if (this.title != null) {
                        output.writeMessage(1, this.title);
                    }
                    if (this.subtitle != null) {
                        output.writeMessage(2, this.subtitle);
                    }
                    super.writeTo(output);
                }

                protected int computeSerializedSize() {
                    int size = super.computeSerializedSize();
                    if (this.title != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(1, this.title);
                    }
                    if (this.subtitle != null) {
                        return size + CodedOutputByteBufferNano.computeMessageSize(2, this.subtitle);
                    }
                    return size;
                }

                public Message mergeFrom(CodedInputByteBufferNano input) throws IOException {
                    while (true) {
                        int tag = input.readTag();
                        if (tag == 0) {
                            return this;
                        }
                        if (tag == 10) {
                            if (this.title == null) {
                                this.title = new FormattedText();
                            }
                            input.readMessage(this.title);
                        } else if (tag == 18) {
                            if (this.subtitle == null) {
                                this.subtitle = new FormattedText();
                            }
                            input.readMessage(this.subtitle);
                        } else if (!WireFormatNano.parseUnknownField(input, tag)) {
                            return this;
                        }
                    }
                }
            }

            public static final class TapAction extends MessageNano {
                public int actionType;
                public String intent;

                public TapAction() {
                    clear();
                }

                public TapAction clear() {
                    this.actionType = 0;
                    this.intent = "";
                    this.cachedSize = -1;
                    return this;
                }

                public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                    if (this.actionType != 0) {
                        output.writeInt32(1, this.actionType);
                    }
                    if (!this.intent.equals("")) {
                        output.writeString(2, this.intent);
                    }
                    super.writeTo(output);
                }

                protected int computeSerializedSize() {
                    int size = super.computeSerializedSize();
                    if (this.actionType != 0) {
                        size += CodedOutputByteBufferNano.computeInt32Size(1, this.actionType);
                    }
                    if (this.intent.equals("")) {
                        return size;
                    }
                    return size + CodedOutputByteBufferNano.computeStringSize(2, this.intent);
                }

                public TapAction mergeFrom(CodedInputByteBufferNano input) throws IOException {
                    while (true) {
                        int tag = input.readTag();
                        if (tag != 0) {
                            if (tag == 8) {
                                int value = input.readInt32();
                                switch (value) {
                                    case 0:
                                    case 1:
                                    case 2:
                                        this.actionType = value;
                                        break;
                                    default:
                                        break;
                                }
                            } else if (tag == 18) {
                                this.intent = input.readString();
                            } else if (!WireFormatNano.parseUnknownField(input, tag)) {
                                return this;
                            }
                        } else {
                            return this;
                        }
                    }
                }
            }

            public static SmartspaceCard[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new SmartspaceCard[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public SmartspaceCard() {
                clear();
            }

            public SmartspaceCard clear() {
                this.shouldDiscard = false;
                this.cardPriority = 0;
                this.cardId = 0;
                this.preEvent = null;
                this.duringEvent = null;
                this.postEvent = null;
                this.icon = null;
                this.cardType = 0;
                this.tapAction = null;
                this.updateTimeMillis = 0;
                this.eventTimeMillis = 0;
                this.eventDurationMillis = 0;
                this.expiryCriteria = null;
                this.cachedSize = -1;
                return this;
            }

            public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                if (this.shouldDiscard) {
                    output.writeBool(1, this.shouldDiscard);
                }
                if (this.cardId != 0) {
                    output.writeInt32(2, this.cardId);
                }
                if (this.preEvent != null) {
                    output.writeMessage(3, this.preEvent);
                }
                if (this.duringEvent != null) {
                    output.writeMessage(4, this.duringEvent);
                }
                if (this.postEvent != null) {
                    output.writeMessage(5, this.postEvent);
                }
                if (this.icon != null) {
                    output.writeMessage(6, this.icon);
                }
                if (this.cardType != 0) {
                    output.writeInt32(7, this.cardType);
                }
                if (this.tapAction != null) {
                    output.writeMessage(8, this.tapAction);
                }
                if (this.updateTimeMillis != 0) {
                    output.writeInt64(9, this.updateTimeMillis);
                }
                if (this.eventTimeMillis != 0) {
                    output.writeInt64(10, this.eventTimeMillis);
                }
                if (this.eventDurationMillis != 0) {
                    output.writeInt64(11, this.eventDurationMillis);
                }
                if (this.expiryCriteria != null) {
                    output.writeMessage(12, this.expiryCriteria);
                }
                if (this.cardPriority != 0) {
                    output.writeInt32(13, this.cardPriority);
                }
                super.writeTo(output);
            }

            protected int computeSerializedSize() {
                int size = super.computeSerializedSize();
                if (this.shouldDiscard) {
                    size += CodedOutputByteBufferNano.computeBoolSize(1, this.shouldDiscard);
                }
                if (this.cardId != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(2, this.cardId);
                }
                if (this.preEvent != null) {
                    size += CodedOutputByteBufferNano.computeMessageSize(3, this.preEvent);
                }
                if (this.duringEvent != null) {
                    size += CodedOutputByteBufferNano.computeMessageSize(4, this.duringEvent);
                }
                if (this.postEvent != null) {
                    size += CodedOutputByteBufferNano.computeMessageSize(5, this.postEvent);
                }
                if (this.icon != null) {
                    size += CodedOutputByteBufferNano.computeMessageSize(6, this.icon);
                }
                if (this.cardType != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(7, this.cardType);
                }
                if (this.tapAction != null) {
                    size += CodedOutputByteBufferNano.computeMessageSize(8, this.tapAction);
                }
                if (this.updateTimeMillis != 0) {
                    size += CodedOutputByteBufferNano.computeInt64Size(9, this.updateTimeMillis);
                }
                if (this.eventTimeMillis != 0) {
                    size += CodedOutputByteBufferNano.computeInt64Size(10, this.eventTimeMillis);
                }
                if (this.eventDurationMillis != 0) {
                    size += CodedOutputByteBufferNano.computeInt64Size(11, this.eventDurationMillis);
                }
                if (this.expiryCriteria != null) {
                    size += CodedOutputByteBufferNano.computeMessageSize(12, this.expiryCriteria);
                }
                if (this.cardPriority != 0) {
                    return size + CodedOutputByteBufferNano.computeInt32Size(13, this.cardPriority);
                }
                return size;
            }

            public SmartspaceCard mergeFrom(CodedInputByteBufferNano input) throws IOException {
                while (true) {
                    int tag = input.readTag();
                    int value;
                    switch (tag) {
                        case 0:
                            return this;
                        case 8:
                            this.shouldDiscard = input.readBool();
                            break;
                        case 16:
                            this.cardId = input.readInt32();
                            break;
                        case 26:
                            if (this.preEvent == null) {
                                this.preEvent = new Message();
                            }
                            input.readMessage(this.preEvent);
                            break;
                        case 34:
                            if (this.duringEvent == null) {
                                this.duringEvent = new Message();
                            }
                            input.readMessage(this.duringEvent);
                            break;
                        case 42:
                            if (this.postEvent == null) {
                                this.postEvent = new Message();
                            }
                            input.readMessage(this.postEvent);
                            break;
                        case 50:
                            if (this.icon == null) {
                                this.icon = new Image();
                            }
                            input.readMessage(this.icon);
                            break;
                        case 56:
                            value = input.readInt32();
                            switch (value) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                case 4:
                                case 5:
                                case 6:
                                    this.cardType = value;
                                    break;
                                default:
                                    break;
                            }
                        case R.styleable.AppCompatTheme_editTextBackground /*66*/:
                            if (this.tapAction == null) {
                                this.tapAction = new TapAction();
                            }
                            input.readMessage(this.tapAction);
                            break;
                        case R.styleable.AppCompatTheme_listDividerAlertDialog /*72*/:
                            this.updateTimeMillis = input.readInt64();
                            break;
                        case 80:
                            this.eventTimeMillis = input.readInt64();
                            break;
                        case R.styleable.AppCompatTheme_ratingBarStyleSmall /*88*/:
                            this.eventDurationMillis = input.readInt64();
                            break;
                        case R.styleable.AppCompatTheme_textAppearanceListItemSecondary /*98*/:
                            if (this.expiryCriteria == null) {
                                this.expiryCriteria = new ExpiryCriteria();
                            }
                            input.readMessage(this.expiryCriteria);
                            break;
                        case R.styleable.AppCompatTheme_textColorAlertDialogListItem /*104*/:
                            value = input.readInt32();
                            switch (value) {
                                case 0:
                                case 1:
                                case 2:
                                    this.cardPriority = value;
                                    break;
                                default:
                                    break;
                            }
                        default:
                            if (WireFormatNano.parseUnknownField(input, tag)) {
                                break;
                            }
                            return this;
                    }
                }
            }
        }

        public SmartspaceUpdate() {
            clear();
        }

        public SmartspaceUpdate clear() {
            this.card = SmartspaceCard.emptyArray();
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.card != null && this.card.length > 0) {
                for (SmartspaceCard element : this.card) {
                    if (element != null) {
                        output.writeMessage(1, element);
                    }
                }
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.card != null && this.card.length > 0) {
                for (SmartspaceCard element : this.card) {
                    if (element != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(1, element);
                    }
                }
            }
            return size;
        }

        public SmartspaceUpdate mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                if (tag == 0) {
                    return this;
                }
                if (tag == 10) {
                    int arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 10);
                    int i = this.card == null ? 0 : this.card.length;
                    SmartspaceCard[] newArray = new SmartspaceCard[(i + arrayLength)];
                    if (i != 0) {
                        System.arraycopy(this.card, 0, newArray, 0, i);
                    }
                    while (i < newArray.length - 1) {
                        newArray[i] = new SmartspaceCard();
                        input.readMessage(newArray[i]);
                        input.readTag();
                        i++;
                    }
                    newArray[i] = new SmartspaceCard();
                    input.readMessage(newArray[i]);
                    this.card = newArray;
                } else if (!WireFormatNano.parseUnknownField(input, tag)) {
                    return this;
                }
            }
        }
    }
}
